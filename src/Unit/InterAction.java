package Unit;

/**
 * Created by snowf on 2019/2/17.
 */
public class InterAction implements Comparable<InterAction> {
    private ChrRegion Left;
    private ChrRegion Right;
    //    public String Name;
    public int Score;
//    public int LeftFragment;
//    public int RightFragment;
//    public boolean SortByName = false;

    public InterAction(ChrRegion reg1, ChrRegion reg2) {
        Left = reg1;
        Right = reg2;
    }

    public InterAction(ChrRegion reg1, ChrRegion reg2, int score) {
        Left = reg1;
        Right = reg2;
        Score = score;
    }

    public InterAction(String[] s) {
        Left = new ChrRegion(s[0], Integer.parseInt(s[1]), Integer.parseInt(s[2]));
        Right = new ChrRegion(s[3], Integer.parseInt(s[4]), Integer.parseInt(s[5]));
    }

    public boolean IsBelong(InterAction action) {
        return Left.IsBelong(action.getLeft()) && Right.IsBelong(action.getRight());
    }

    public ChrRegion getLeft() {
        return Left;
    }

    public ChrRegion getRight() {
        return Right;
    }

    @Override
    public String toString() {
        return Left.toString() + "\t" + Right.toString();
    }

    @Override
    public int compareTo(InterAction o) {
        int res = Left.compareTo(o.getLeft());
        if (res == 0) {
            return Right.compareTo(o.getRight());
        } else {
            return res;
        }
    }

    public int Distance() {
        return Left.Distance(Right);
    }
}
