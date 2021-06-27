package antsuabon.guribiSKSolver;

import gurobi.*;
import java.io.*;
import java.util.List;
import java.util.Map;

import antsuabon.guribiSKSolver.SudokuProblem.Position;
import antsuabon.guribiSKSolver.utils.SudokuLoader;

public class GurobiSKSolver {

	private static String NAME_WITHOUT_EXTENSION = "S_2x4_easy_1";

	public static void main(String[] args) {

		// Carga de los datos iniciales del problema
		SudokuProblem sudoku = SudokuLoader.loadSudoku("resources/" + NAME_WITHOUT_EXTENSION + ".txt");
		Integer n = Math.max(sudoku.getRows(), sudoku.getCols());

		try {

			// Inicialización del entorno y el modelo
			GRBEnv env = new GRBEnv();
			GRBModel model = new GRBModel(env);
			// model.set(GRB.IntParam.Threads, 8);

			// Definición de las variables binarias
			GRBVar[][][] vars = new GRBVar[n][n][n];

			for (int i = 0; i < sudoku.getRows(); i++) {
				for (int j = 0; j < sudoku.getRows(); j++) {
					for (int v = 0; v < n; v++) {
						String st = "G_" + String.valueOf(i) + "_" + String.valueOf(j) + "_" + String.valueOf(v);
						vars[i][j][v] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
					}
				}
			}

			// Variable reutilizable, usada para definir las restricciones
			GRBLinExpr expr;

			// Restricción: Cada casilla solo puede contener una cifra
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					expr = new GRBLinExpr();
					expr.addTerms(null, vars[i][j]);
					String st = "V_" + String.valueOf(i) + "_" + String.valueOf(j);
					model.addConstr(expr, GRB.EQUAL, 1.0, st);
				}
			}

			// Restricción: Cada cifra solo puede aparecer una vez por fila
			for (int i = 0; i < n; i++) {
				for (int v = 0; v < n; v++) {
					expr = new GRBLinExpr();
					for (int j = 0; j < n; j++)
						expr.addTerm(1.0, vars[i][j][v]);
					String st = "R_" + String.valueOf(i) + "_" + String.valueOf(v);
					model.addConstr(expr, GRB.EQUAL, 1.0, st);
				}
			}

			// Restricción: Cada cifra solo puede aparecer una vez por columna
			for (int j = 0; j < n; j++) {
				for (int v = 0; v < n; v++) {
					expr = new GRBLinExpr();
					for (int i = 0; i < n; i++)
						expr.addTerm(1.0, vars[i][j][v]);
					String st = "C_" + String.valueOf(j) + "_" + String.valueOf(v);
					model.addConstr(expr, GRB.EQUAL, 1.0, st);
				}
			}

			// Restricción: Una cifra solo puede aparecer una vez por región
			for (int v = 0; v < n; v++) {
				for (int i0 = 0; i0 < sudoku.getRows() / sudoku.getRegionY(); i0++) {
					for (int j0 = 0; j0 < sudoku.getCols() / sudoku.getRegionX(); j0++) {
						expr = new GRBLinExpr();
						
						for (int k = 0; k < sudoku.getRegionY(); k++) {
							for (int k2 = 0; k2 < sudoku.getRegionX(); k2++) {
								Integer i = sudoku.getRegionY() * i0 + k;
								Integer j = sudoku.getRegionX() * j0 + k2;
								
								expr.addTerm(1.0, vars[i][j][v]);
							}
						}
						
						String st = "B_" + String.valueOf(v) + "_" + String.valueOf(i0) + "_" + String.valueOf(j0);
						model.addConstr(expr, GRB.EQUAL, 1.0, st);
					}
				}
			}

			// La suma de cada sector punteado debe ser igual al indicado en el fichero del
			// problema
			int blockIndex = 0;
			for (Map.Entry<List<SudokuProblem.Position>, Integer> entry : sudoku.getBlocks().entrySet()) {
				expr = new GRBLinExpr();

				for (Position position : entry.getKey()) {
					for (int v = 0; v < n; v++)
						expr.addTerm(v + 1, vars[position.getI()][position.getJ()][v]);
				}
				String st = "S_" + String.valueOf(blockIndex);
				model.addConstr(expr, GRB.EQUAL, entry.getValue(), st);
				blockIndex++;
			}

			// Aquella variables que pertenecen a cifras contenidas en la definicón de
			// problema seran inicializadas como verdaderas
			for (int i = 0; i < sudoku.getRows(); i++) {
				for (int j = 0; j < sudoku.getCols(); j++) {
					Integer val = sudoku.getInitialState()[i][j];
					if (val > 0)
						vars[i][j][val - 1].set(GRB.DoubleAttr.LB, 1.0);
				}
			}

			// Optimize model

			model.optimize();

			// Escritura del modelo en un archivo
			model.write("lp/" + NAME_WITHOUT_EXTENSION + ".lp");

			// Visualización de los resultados
			double[][][] x = model.get(GRB.DoubleAttr.X, vars);

			System.out.println();
			for (int i = 0; i < sudoku.getRows(); i++) {
				for (int j = 0; j < sudoku.getCols(); j++) {
					for (int v = 0; v < n; v++) {
						if (x[i][j][v] > 0.5) {
							if (j == sudoku.getCols() - 1) {
								System.out.print(v + 1);
							} else {
								System.out.print((v + 1) + ", ");
							}
						}
					}
				}
				System.out.println();
			}

			// Cierre del modelo y del entorno
			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
		}

	}
}
