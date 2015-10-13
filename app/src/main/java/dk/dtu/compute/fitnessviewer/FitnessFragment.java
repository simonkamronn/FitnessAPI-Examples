package dk.dtu.compute.fitnessviewer;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

/**
This fragment is basically just a front to show the graphs
 **/

public class FitnessFragment extends Fragment {
    private static final String TAG = FitnessFragment.class.getSimpleName();
    private FitnessListener activityCallback;
    public static final String GET_STEPS = "getSteps";
    public static final String BAR_CHART_CLICKED = "barChart";
    public static final String STACKED_BAR_CHART_CLICKED = "stackedBarChart";
    public static final String GET_POWER = "getPower";
    public static final String GET_ACTIVITY = "getActivity";

    public ColumnChartView mColumnChart;
    public ColumnChartData mColumnData;
    public LineChartView mLineChart;
    public LineChartData mLineData;
    public LineChartView mCalorieChart;
    public LineChartData mCalorieData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Put application specific code here.
    }

    public FitnessFragment(){
        super();
        // empty constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
    }

    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fitness, container, false);
        mColumnChart = (ColumnChartView) view.findViewById(R.id.steps_chart);
        mLineChart = (LineChartView) view.findViewById(R.id.activity_chart);
        mCalorieChart = (LineChartView) view.findViewById(R.id.calorie_chart);

        return view;
    }

    public interface FitnessListener {
        public void onButtonClick(String text, int position);
    }

    /**
     * Called when a fragment is first attached to its activity.
     * {@link #onCreate(Bundle)} will be called after this.
     *
     * @param activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            activityCallback = (FitnessListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ToolbarListener");
        }
    }

    public void setStepsData(List<AxisValue> xAxisValues, List<Column> columns){
        mColumnData = new ColumnChartData(columns);
        Axis axisX = new Axis(xAxisValues);
        Axis axisY = new Axis().setHasLines(true);
        axisY.setMaxLabelChars(5);
        mColumnData.setAxisXBottom(axisX);
        mColumnData.setAxisYLeft(axisY);

        mColumnChart.setColumnChartData(mColumnData);
    }
    public void setActivityData(List<AxisValue> xAxisValues, List<Line> lines){
        mLineData = new LineChartData(lines);
        mLineData = setAxis(xAxisValues, mLineData);
        mLineData.setBaseValue(Float.NEGATIVE_INFINITY);
        mLineChart.setLineChartData(mLineData);
    }

    public void setCalorieData(List<AxisValue> xAxisValues, List<Line> lines){
        mCalorieData = new LineChartData(lines);
        mCalorieData = setAxis(xAxisValues, mCalorieData);
        mCalorieData.setBaseValue(Float.NEGATIVE_INFINITY);
        mCalorieChart.setLineChartData(mCalorieData);
    }

    private LineChartData setAxis(List<AxisValue> xAxisValues, LineChartData chartData){
        // Axes
        Axis axisX = new Axis(xAxisValues);
        Axis axisY = new Axis().setHasLines(true);
        axisY.setMaxLabelChars(4);
        chartData.setAxisXBottom(axisX);
        chartData.setAxisYLeft(axisY);

        return chartData;
    }
}
