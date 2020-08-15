#!/usr/bin/env python3

# Bardzo prosty skrypt, który rozwiązuje to samo
# zadanie, dla takiego samego formatu wejściowego,
# ale brute-forcem. O(n^2)
#
# ./solver.py 2 small2D.csv

import argparse
import csv


def readCSV(file):
	res = []
	with open(file) as csv_file:
		reader = csv.reader(csv_file, delimiter=';')
		for line in reader:
			if line:
				res.append(line)
	return res


def convertData(point):
	res = []
	for x in point:
		if x == 'D':
			res.append('D')
		elif x == 'Q':
			res.append('Q')
		else:
			res.append(int(x))
	return res


def solve(dimensions, data):
	queries = []
	points = []
	for line in map(convertData, data):
		if line[0] == 'Q':
			queries.append(line)
		if line[0] == 'D':
			points.append(line)

	for q in queries:
		counter = 0
		for p in points:
			isOk = True
			# Sprawdz wszystkie wspołrzędne.
			for i in range(1, dimensions+1):
				if not p[i] < q[i]:
					isOk = False
			if isOk:
				counter = counter + 1
		for i in range(1, dimensions+1):
			print(q[i], end=';')
		print(counter, end='\n')


def main():
	parser = argparse.ArgumentParser()
	parser.add_argument("dimensions", type=int)
	parser.add_argument("file")
	args = parser.parse_args()

	data = readCSV(args.file)

	solve(args.dimensions, data)


main()
