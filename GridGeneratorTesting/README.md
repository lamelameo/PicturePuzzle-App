# Testing the function used to generate randomised grids

For 3x3 and 4x4 grid sizes, the fucntion creates adequately random combinations, tested by generating ridiculously large amounts...

4x4 grid: tested 1 million generated grids.
Created 999,999 unique grids (0.000015% of total possible) and 1 duplicate.

Stats Output:

![alt_text](4x4%201mil%20unique%20grids.png)

3x3 grid: tested 10 million generated grids. 
Expect average frequency of 10,000,000/20,160 = 496, if each grid is equally likely. Made all possible solvable, unique grids with a mean frequency of 496, spread in a narrow standard distribution (std. dev. = 22, range of 183 frequency values). 

Stats Output:

![alt_text](3x3%2010mil%20unique%20grids.png)

![alt_text](3x3%2010%20mil%20dataset%20stats.png)

Scatter Plot:

![alt_text](3x3grid10mil%20scatter.png)

Histogram:
![alt_text](3x3%2010mil%20histogram1.png)
