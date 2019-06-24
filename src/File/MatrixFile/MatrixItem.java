package File.MatrixFile;

import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

/**
 * Created by 浩 on 2019/2/1.
 */
public class MatrixItem extends Array2DRowRealMatrix implements Comparable<MatrixItem> {
    public MatrixItem(int rowDimension, int columnDimension) throws NotStrictlyPositiveException {
        super(rowDimension, columnDimension);
    }

    @Override
    public int compareTo(MatrixItem o) {
        return 0;
    }
}
