# SpreadsheetEvaluator

**Background**: The rows in a spreadsheet are labelled from A-Z and columns are numbered from 1. Thus, if there are 3 rows and 2 columns, the cells are denoted by A1, A2, B1, B2, C1, C2.

The code takes in a spreadsheet, where the input is in the following format:

Line 1: COLUMN_COUNT ROW_COUNT

Lines [2 - ROW_COUNTxCOLUMN_COUNT+1] contain expressions in Reverse Polish Notation present in each cell of the spreadsheet

The output contains ROW_COUNTxCOLUMN_COUNT lines, each containing the final evaluated expression of each cell in the spreadsheet. The orders of the input expressions and output values are as depicted in the example above.

If the spreadsheet has cyclic dependencies, an error is thrown stating so.
