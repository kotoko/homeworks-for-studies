#!/usr/bin/env python3
# Wygeneruj losowe dane z zapytaniami w 3D.
#

from random import randint
POINTS_SIZE = 15 * 15 * 15
MAX_POINT_VALUE = int(1.8 * POINTS_SIZE)


def genPoints(size):
	res = {}
	while len(res) < size:
		x = randint(0, MAX_POINT_VALUE)
		y = randint(0, MAX_POINT_VALUE)
		z = randint(0, MAX_POINT_VALUE)
		res[(x, y, z)] = True

	return list(map(lambda a: (a[0], a[1], a[2]), res))


def printData(points):
	for xs in points:
		print("D;{};{};{}".format(xs[0], xs[1], xs[2]), end='\n')


def printQueries(points):
	for xs in points:
		print("Q;{};{};{}".format(xs[0], xs[1], xs[2]), end='\n')


def main():
	mode = randint(0, 1)
	points = genPoints(POINTS_SIZE)
	printData(points)
	if mode == 0:
		printQueries(points)
	else:
		quieries = genPoints(POINTS_SIZE / 20)
		printQueries(quieries)


main()
