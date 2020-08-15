import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{LongType, StringType, StructField, StructType}

import scala.collection.mutable
import scala.reflect.ClassTag

// Żeby móc porównać tuple stringów operatorem '<'
// https://stackoverflow.com/a/11104558
import scala.math.Ordered.orderingToOrdered


object Duze2 {
	type PointValueType = Long
	type Point = Product
	type PointSpecies = Int
	type OldLabel = (String, String)
	type NewLabel = OldLabel

	val emptyLabel = ("", "") : OldLabel

	var chunks = 8  // Na ile części dzielić dane prz pracy na wielu komputerach.
	val D_SPECIES = 1  // Punkt, który jest daną wejściową.
	val Q_SPECIES = 0  // Punkt, który jest zapytaniem.
	assert(D_SPECIES > Q_SPECIES)  // Potrzebne do sortowania punktów.

	def run(args: Array[String]): Unit = {
		val (dimensions, csvInPath, csvOutPath) = parseArgs(args)

		val spark = SparkSession.builder.appName("Duze2").getOrCreate()
		val sc = spark.sparkContext
		setNativeChunkNumber(sc)

		val csv = spark.read.format("csv")
			.option("sep", ";")
			.option("header", "false")
			.option("mode", "FAILFAST")
			.schema(createSchemaType(dimensions))
			.load(csvInPath)
			.rdd

		dimensions match {
			case 2 => run2D(sc, csv, csvOutPath)
			case 3 => run3D(sc, csv, csvOutPath)
		}
	}

	def run3D(
		sc : SparkContext,
		csv : org.apache.spark.rdd.RDD[org.apache.spark.sql.Row],
		csvOutPath : String
	) : Unit = {
		val csvNormalized = csv
			.map(x => (
				(x.getLong(1), x.getLong(2), x.getLong(3)),
				emptyLabel,
				(if (x.getString(0)(0) == 'D') D_SPECIES else Q_SPECIES)
			))
			.asInstanceOf[org.apache.spark.rdd.RDD[(Point, OldLabel, PointSpecies)]]

		val firstReduction = stepReduction(0, csvNormalized, {case((_, ly) , newL) => (newL, ly)}, sc)
		val secondReduction = stepReduction(1, firstReduction, {case((lx, _) , newL) => (lx, newL)}, sc)
		val thirdReduction = stepFinal(2, secondReduction, csvNormalized, sc)
			.asInstanceOf[org.apache.spark.rdd.RDD[((PointValueType, PointValueType, PointValueType), Long)]]
			.reduceByKey(_ + _)

		thirdReduction
			.map {case(xs, c) => (
				xs.productElement(0).asInstanceOf[PointValueType].toString
				+ ";" + xs.productElement(1).asInstanceOf[PointValueType].toString
				+ ";" + xs.productElement(2).asInstanceOf[PointValueType].toString
				+ ";" + c
			)}
			.saveAsTextFile(csvOutPath)
	}

	def run2D
	(
		sc : SparkContext,
		csv : org.apache.spark.rdd.RDD[org.apache.spark.sql.Row],
		csvOutPath : String
	) : Unit = {
		val csvNormalized = csv
			.map(x => (
				(x.getLong(1), x.getLong(2)),
				emptyLabel,
				(if (x.getString(0)(0) == 'D') D_SPECIES else Q_SPECIES)
			))
			.asInstanceOf[org.apache.spark.rdd.RDD[(Point, OldLabel, PointSpecies)]]

		val firstReduction = stepReduction(0, csvNormalized, {case((_, ly) , newL) => (newL, ly)}, sc)

		val secondReduction = stepFinal(1, firstReduction, csvNormalized, sc)
			.asInstanceOf[org.apache.spark.rdd.RDD[((PointValueType, PointValueType), Long)]]
			.reduceByKey(_ + _)

		secondReduction
			.map {case(xs, c) => (
				xs.productElement(0).asInstanceOf[PointValueType].toString
				+ ";" + xs.productElement(1).asInstanceOf[PointValueType].toString
				+ ";" + c
			)}
			.saveAsTextFile(csvOutPath)
	}

	// [(xs, oldLabel, s)] ~> [(xs, newLabel, s)]
	def stepReduction
	(
		dimension: Int,  // Counting from 0 to (n-1).
		data : org.apache.spark.rdd.RDD[(Point, OldLabel, PointSpecies)],
		updateLabel : (OldLabel, String) => NewLabel,
		sc : SparkContext
	) : org.apache.spark.rdd.RDD[(Point, NewLabel, PointSpecies)] = {
		val dataSorted = data.sortBy {case(xs, l, s) => (l, xs.productElement(dimension).asInstanceOf[PointValueType], s)}
		val dataLen = dataSorted.count

		// Oblicz dla każdej etykiety liczbę elementów w jej obrębie.
		val dataIndexesCount = dataSorted
			.map{case(_, l, _) => (l, 1 : Long)}
			.reduceByKey(_ + _)
			// [(l, counter)]

		// Oblicz pary (początkowa etykieta, końcowa etykieta) dla każdego komputera.
		// Potrzebne, żeby wiedzieć jak podzielić zbiór posumowanych wystąpień etykiet.
		dataSorted.cache
		val dataIndexesBoundaries = minAlgoGroupBy(dataSorted, dataLen)
			.map {case (index, list) => {
				var firstL = None : Option[OldLabel]
				var lastL = None : Option[OldLabel]

				for((_, l, _) <- list) {
					firstL match {
						case None => firstL = Some(l)
						case _ => {}
					}

					lastL = Some(l)
				}

				(firstL, lastL) match {
					case (Some(firstLValue), Some(lastLValue)) => (index, firstLValue, lastLValue) : (Long, OldLabel, OldLabel)
					case _ => (null.asInstanceOf[Long], null.asInstanceOf[OldLabel], null.asInstanceOf[OldLabel]) : (Long, OldLabel, OldLabel)
				}
			}}
			.sortBy {case (i, _, _) => i}
			.map {case (_, first, last) => (first, last)}
			.collect  // index -> (firstLabel, lastLabel)

		val dataIndexesBoundariesBroadcast = sc.broadcast(dataIndexesBoundaries)

		// Poszerzenie zbioru 'dataSorted' o informację ile jest wszystkich
		// elementów z daną etykietą -> [(xs, l, s, total_number_of_elements_with_my_label)].
		val dataIndexesCountPartitioned = dataIndexesCount
			.flatMap {case (label, counter) => {
				var res = List() : List[Long]  // (komputer, (etykieta, suma))

				// Jeśli dana etykieta będzie w i-tym komputerze, to wyślij tam info
				// o (wcześniej) zsumowanej liczbie wystąpień tej etykiety.
				for(((first, last), index) <- dataIndexesBoundariesBroadcast.value.zipWithIndex) {
					if(first <= label && label <= last) {
						res = index.longValue :: res
					}
				}

				res.map(index => (index, (label, counter)))
			}}

		// Uzgadnianie do najmniejszego wspólnego typu.
		val dataSortedUnion = dataSorted.zipWithIndex.map {case ((xs, l, s), index) => {
			(calcChunk(index, dataLen), (
				"data",
				(xs, l, s),
				(null.asInstanceOf[OldLabel], null.asInstanceOf[Long])
			))
		}}
		val dataIndexesCountPartitionedUnion = dataIndexesCountPartitioned.map {case (index, (l, counter)) => {
			(index, (
				"labelCounter",
				(null.asInstanceOf[Point], null.asInstanceOf[OldLabel], null.asInstanceOf[PointSpecies]),
				(l, counter)
			))
		}}

		// Dopisz informacje o rozmiarach etykiet.
		val dataSortedWithSizes = dataSortedUnion.union(dataIndexesCountPartitionedUnion)
			.groupByKey
			.flatMap {case (index, list) => {
				// Mapa: label -> counter
				var labels = mutable.HashMap() : scala.collection.mutable.Map[OldLabel, Long]

				// Wrzuć do mapy wszystkie liczniki.
				for((unionName, _, (l, c)) <- list if unionName == "labelCounter") {
					labels = labels + (l -> c)
				}

				var res = List() : List[(Point, OldLabel, PointSpecies, Long)]

				// Dopisz liczniki do danych.
				for((unionName, (xs, l, s), _) <- list if unionName == "data") {
					res = (xs, l, s, labels(l)) :: res
				}

				res
			}}
			.sortBy {case (xs, l, s, _) => (l, xs.productElement(dimension).asInstanceOf[PointValueType], s)}
			// [(xs, l, s, size)]

		// Policz wartości pomocnicze do obliczania indeksów.
		// Ile razy ostatnia etykieta pojawia się na danym komputerze.
		dataSortedWithSizes.cache
		val dataSortedWithSizesHelper = minAlgoGroupBy(dataSortedWithSizes, dataLen)
			.map {case (index, list) => {
				var lastLabel = None : Option[OldLabel]
				var counter = None : Option[Long]

				for((_, l, _, _) <- list) {
					(lastLabel, counter) match {
						case (Some(lastLabelValue), Some(counterValue)) => {
							if(lastLabelValue == l) {
								counter = Some(counterValue + 1)
							}
							else {
								lastLabel = Some(l)
								counter = Some(1)
							}
						}
						case (None, None) => {
							lastLabel = Some(l)
							counter = Some(1)
						}
						case _ => {}
					}
				}

				(lastLabel, counter) match {
					case (Some(lastLabelValue), Some(counterValue)) => (index, lastLabelValue, counterValue)
					case _ => (index, null.asInstanceOf[OldLabel], null.asInstanceOf[Long])
				}
			}}
			.sortBy {case (i, _, _) => i}
			.map {case (_, l, size) => (l, size)}
			.collect

		val dataSortedWithSizesHelperBroadcast = sc.broadcast(dataSortedWithSizesHelper)

		// Nadaj indeksy z drzewa.
		val dataIndexed = minAlgoGroupBy(dataSortedWithSizes, dataLen)
			.flatMap{case (index, list) => {
				var firstLabel = None : Option[OldLabel]

				// Znajdź pierwszą etykietę.
				for((_, l, _, _) <- list) {
					firstLabel match {
						case None => firstLabel = Some(l)
						case _ => {}
					}
				}

				// Dodaj z poprzednich komputerów liczbę elementów o tej samej etykiecie.
				var prevSum = 0 : Long
				firstLabel match {
					case Some(firstLabelValue) => {
						for(i <- 0 until index.toInt) {
							val (prevLabel, prevCounter) = dataSortedWithSizesHelperBroadcast.value(i)
							if(prevLabel == firstLabelValue) {
								prevSum += prevCounter
							}
						}
					}
					case _ => {}
				}

				// Policz indeksy w drzewie.
				var res = List() : List[List[(Point, NewLabel, PointSpecies)]]
				var currentIndex = prevSum
				var currentLabel = firstLabel
				for((xs, l, s, size) <- list) {
					currentLabel match {
						case Some(currentLabelValue) => {
							if(currentLabelValue != l) {
								currentLabel = Some(l)
								currentIndex = 0
							}

							val treeIndexLength = calcTreeIndexLen(size - 1)
							val treeIndex = makeTreeIndex(currentIndex, treeIndexLength)
							val prefixes = calcPrefixes(treeIndex, s)

							res = prefixes.map(p => (xs, updateLabel(l, p), s)) :: res

							currentIndex = currentIndex + 1
						}
						case _ => {}
					}
				}

				res.flatten
			}}

		dataIndexed
	}

	// [(xs, oldLabel, s)] ~> [(xs, counter)]
	def stepFinal(
		dimension: Int,  // Counting from 0 to (n-1).
		data : org.apache.spark.rdd.RDD[(Point, OldLabel, PointSpecies)],
		originalData : org.apache.spark.rdd.RDD[(Point, OldLabel, PointSpecies)],
		sc : SparkContext
	) : org.apache.spark.rdd.RDD[(Point, Long)] = {
		val dataSorted = data.sortBy {case(xs, l, s) => (l, xs.productElement(dimension).asInstanceOf[PointValueType], s)}
		dataSorted.cache
		val dataLen = dataSorted.count

		// Policz sumy na końcówce każdego komputera.
		dataSorted.cache
		val dataHelper = minAlgoGroupBy(dataSorted, dataLen)
			.map {case (index, list) => {
				var lastLabel = None : Option[OldLabel]
				var counter = 0 : Long

				// Oblicz liczbę powtórzeń ostatniej etykiety na tym komputerze.
				// Tylko punkty z gatunku "DATA"
				for((_, l, s) <- list) {
					lastLabel match {
						case None => {
							lastLabel = Some(l)
							counter = if(s == D_SPECIES) 1 else 0
						}
						case Some(prevL) => {
							if(prevL != l) {
								lastLabel = Some(l)
								counter = 0
							}

							counter = counter + (if(s == D_SPECIES) 1 else 0)
						}
					}
				}

				lastLabel match {
					case Some(prevL) => {
						(index, prevL, counter) : (Long, OldLabel, Long)
					}
					case _ => (null.asInstanceOf[Long], null.asInstanceOf[OldLabel], null.asInstanceOf[Long]) : (Long, OldLabel, Long)
				}
			}}
			.sortBy {case (i, _, _) => i}
			.map {case (_, l, c) => (l, c)}
			.collect

		val dataHelperBroadcast = sc.broadcast(dataHelper)

		val answerPartial = minAlgoGroupBy(dataSorted, dataLen)
			.flatMap {case (index, list) => {
				var res = List() : List[(Point, Long)]
				var counter = 0 : Long
				var currentLabel = None : Option[OldLabel]

				for((xs,l,s) <- list) {
					currentLabel match {
						case None => {
							// Dodaj wynik z poprzednich komputerów jeśli jest zgodność etykiet.
							for(i <- 0 until index.toInt) {
								val (prevL, prevCounter) = dataHelperBroadcast.value(i)
								if(prevL == l) {
									counter = counter + prevCounter
								}
							}

							// Dla danych zwiększ counter.
							if(s == D_SPECIES) {
								counter = counter + 1
							}
							// Dla zapytań zapisz wynik.
							else {
								res = (xs, counter) :: res
							}

							currentLabel = Some(l)
						}
						case Some(currentLabelValue) => {
							if(currentLabelValue != l) {
								currentLabel = Some(l)
								counter = 0
							}

							// Dla danych zwiększ counter.
							if(s == D_SPECIES) {
								counter = counter + 1
							}
							// Dla zapytań zapisz wynik.
							else {
								res = (xs,counter) :: res
							}
						}
					}
				}

				res
			}}

		val answer = originalData
			.filter {case(_, _, s) => s == Q_SPECIES}
			.map {case(xs, _, _) => (xs, 0:Long)}
			.union(answerPartial)

		// Trzeba jeszcze zrobić .reduceByKey(_ + _) poza tą funkcją.
		// (Problem z typowaniem w scali).
		answer
	}

	// Jakaś ogólna funkcja do alg. minimalnego.
	// Trochę szkoda, że Scala nie ma tak zwanych „extension function”
	// jak Kotlin. Można by się tym fajnie tu w sparku pobawić.
	def minAlgoGroupBy[A : ClassTag](rdd : org.apache.spark.rdd.RDD[A], len : Long) : org.apache.spark.rdd.RDD[(Long, Iterable[A])] = {
		rdd.zipWithIndex.map {case (a, index) => (calcChunk(index, len), a)}.groupByKey
	}

	// Oblicza możliwe prefixy. (Zgodnie z treścią zadania).
	def calcPrefixes(index : String, t : Int) : List[String] = {
		var res = List() : List[String]
		if(t == D_SPECIES) {
			for(i <- 0 until index.length) {
				if(index(i) == '0') {
					res = (index slice(0, i)) :: res
				}
			}
		} else {
			for(i <- 0 until index.length) {
				if(index(i) == '1') {
					res = (index slice(0, i)) :: res
				}
			}
		}

		res
	}

	// Podziel dane na kilka grupek. Dostaje index i liczbę elementów w całym zbiorze.
	def calcChunk(index : Long, len : Long) : Long = {
		// Nie może być więcej "chunków" niż danych.
		val chunksNormalized = if(chunks > len) len else chunks
		val oneChunkLen = (if(len % chunksNormalized != 0) 1 else 0) + len / chunksNormalized
		index / oneChunkLen
	}

	// Oblicza logarytm dwójkowy z danej liczby zaokrąglony w górę.
	def calcTreeIndexLen(len : Long) : Long = {
		var len2 = len
		var counter = 0 : Long

		if(len2 <= 0) {
			return 1
		}

		while(len2 > 0) {
			len2 = len2 / 2
			counter += 1
		}

		counter
	}

	// Zamienia liczbę na stringa. Dodaje padding zer z lewej.
	def makeTreeIndex(index : Long, len : Long) : String = {
		var res = List() : List[Long]
		var index2 = index

		// Kolejne liczby w zapisie binarnym.
		while(index2 > 0) {
			res = (index2 % 2) :: res
			index2 = index2 / 2
		}

		// Padding zer z lewej.
		while(len - res.length > 0) {
			res = 0 :: res
		}

		res.mkString("")
	}

	def createSchemaType(dimensions : Int) : org.apache.spark.sql.types.StructType = {
		var res = List() : List[StructField]
		res = StructField("t", StringType, nullable=false) :: res

		for (i <- 0 until dimensions) {
			res = StructField(s"x$i", LongType, nullable=false) :: res
		}

		StructType(res.reverse.toArray)
	}

	def setNativeChunkNumber(sc : SparkContext) : Unit = {
		// Poczekaj 10 sekund na wszelki wypadek.
		// https://stackoverflow.com/q/51342460
		Thread.sleep(10 * 1000)
		chunks = sc.getExecutorMemoryStatus.size
//		chunks = sc._jsc.sc().getExecutorMemoryStatus.size
	}

	def parseArgs(args : Array[String]) : (Int, String, String) = {
		if(args.length != 3) {
			println("Wrong number of parameters!")
			println(s"Got: ${args.length}")
			println("Expected: 3")
			System.exit(1)
		}

		val dimension = args(0).toInt
		val in = args(1)
		val out = args(2)

		if(in(0) != '/') {
			println("Path to input csv must be absolute! (start with '/')")
			System.exit(1)
		}

		if(out(0) != '/') {
			println("Path to output csv must be absolute! (start with '/')")
			System.exit(1)
		}

		(dimension, in, out)
	}
}
