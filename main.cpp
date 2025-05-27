#include <iostream>
#include <fstream>
using namespace std;

const int N = 9;
int grid[N][N];

bool isSafe(int row, int col, int num)
{
    for (int x = 0; x < N; x++)
    {
        if (grid[row][x] == num || grid[x][col] == num)
            return false;
    }

    int startRow = row - row % 3;
    int startCol = col - col % 3;

    for (int r = 0; r < 3; r++)
        for (int d = 0; d < 3; d++)
            if (grid[r + startRow][d + startCol] == num)
                return false;

    return true;
}

pair<int, int> findMRVCell()
{
    int minCount = N + 1;
    pair<int, int> minCell = make_pair(-1, -1);

    for (int i = 0; i < N; i++)
    {
        for (int j = 0; j < N; j++)
        {
            if (grid[i][j] == 0)
            {
                int count = 0;
                for (int num = 1; num <= N; num++)
                {
                    if (isSafe(i, j, num))
                        count++;
                }
                if (count < minCount)
                {
                    minCount = count;
                    minCell = make_pair(i, j);
                }
            }
        }
    }
    return minCell;
}

bool solveSudoku()
{
    pair<int, int> cell = findMRVCell();
    int row = cell.first, col = cell.second;

    if (row == -1 && col == -1)
        return true;

    for (int num = 1; num <= N; num++)
    {
        if (isSafe(row, col, num))
        {
            grid[row][col] = num;
            if (solveSudoku())
                return true;
            grid[row][col] = 0;
        }
    }
    return false;
}

int main()
{
    ifstream input("input.txt");
    ofstream output("output.txt");

    if (!input || !output)
    {
        cerr << "Error opening input/output file." << endl;
        return 1;
    }

    for (int i = 0; i < N; i++)
        for (int j = 0; j < N; j++)
            input >> grid[i][j];

    if (solveSudoku())
    {
        for (int i = 0; i < N; i++)
        {
            for (int j = 0; j < N; j++)
            {
                output << grid[i][j] << " ";
            }
            output << "\n";
        }
    }
    else
    {
        output << "No solution\n";
    }

    input.close();
    output.close();
    return 0;
}
