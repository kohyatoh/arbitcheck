package arbitcheck.examples.sumstack;

import arbitcheck.Check;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class SumStack {
    private List<Integer> data = new ArrayList<Integer>();
    private int sum = 0;

    public void push(int x) {
        data.add(x);
        sum += x;
    }

    public void pop() {
//        sum -= data.get(data.size()-1);
        data.remove(data.size() - 1);
    }

    public int getSum() {
        return sum;
    }

    public List<Integer> getData() {
//        return Collections.unmodifiableList(data);
        return data;
    }

    @Check
    private void prop_SumData() {
        int _sum = 0;
        for (int x : data) _sum += x;
        assertEquals(_sum, sum);
    }
}

