package com.example.mybill.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mybill.R;
import com.example.mybill.bean.Bill;
import com.example.mybill.bean.Category;
import com.example.mybill.bean.User;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobDate;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListener;


public class BillStatFragment extends Fragment {
    private TextView text_statTitle;
    private TextView text_income;
    private TextView text_outcome;
    private TextView text_EngelCoefficient;
    private TextView text_keyword;
    private TextView text_keyword_comment;
    private TextView text_suggestion;

    private PieChart pieChart_income;
    private PieChart pieChart_outcome;

    private ArrayList<String> categoryName_in;
    private ArrayList<Float> sumAmount_in;
    private float totalAmount_in;

    private ArrayList<String> categoryName_out;
    private ArrayList<Float> sumAmount_out;
    private float totalAmount_out;

    private float totalIncome;
    private float totalOutcome;
    private float engelCoefficient;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.bill_stat_fragment,container,false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //初始化标题行
        text_statTitle = getActivity().findViewById(R.id.text_statTitle);
        String currentUserName = BmobUser.getCurrentUser().getUsername();
        text_statTitle.setText("尊敬的" + currentUserName + "，您的收入和支出情况已经帮你统计出来啦！\n仅有最近一年的数据会被统计");

        //获取总收入支出
        text_income = getActivity().findViewById(R.id.text_income);
        text_outcome = getActivity().findViewById(R.id.text_outcome);
        getTotalIncomeOrOutcome("in");
        getTotalIncomeOrOutcome("out");

        //获取恩格尔系数
        text_EngelCoefficient = getActivity().findViewById(R.id.text_EngelCoefficient);
        getEngelCoefficient();

        //初始化饼图颜色
        final ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.BLUE);
        colors.add(Color.RED);
        colors.add(Color.GREEN);
        colors.add(Color.YELLOW);

        //初始化收入饼图
        categoryName_in = new ArrayList<>();
        sumAmount_in = new ArrayList<>();
        totalAmount_in = 0;
        pieChart_income = getActivity().findViewById(R.id.pieChart_income);
        pieChart_income.setNoDataText("你竟然没有任何收入！");
        pieChart_income.setDrawCenterText(true);  //饼状图中间可以添加文字
        pieChart_income.setDrawHoleEnabled(true);//饼状图中心圆是否存在
        pieChart_income.setCenterText("我的收入");  //饼状图中间的文字
        Description description = new Description();
        description.setText("收入类别统计");
        pieChart_income.setDescription(description);
        String sql = "select sum(amount) from Bill where userId = ? and type = 'in' group by categoryId order by -sum(amount)";
        new BmobQuery<Bill>().doStatisticQuery(sql,new QueryListener<JSONArray>(){
            @Override
            public void done(JSONArray jsonArray, BmobException e) {
                if(e ==null){
                    if(jsonArray != null){
                        if (categoryName_in.size() != 0)
                            categoryName_in.clear();
                        if (sumAmount_in.size() != 0)
                            sumAmount_in.clear();
                        for (int i=0; i<jsonArray.length(); i++){
                            try {
                                final JSONObject value = (JSONObject) jsonArray.get(i);
                                final String categoryId = ((JSONObject)value.get("categoryId")).getString("objectId");
                                final Float sumAmountFloat = Float.parseFloat(value.getString("_sumAmount"));
                                new BmobQuery<Category>().getObject(categoryId, new QueryListener<Category>() {
                                    @Override
                                    public void done(Category category, BmobException e) {
                                        sumAmount_in.add(sumAmountFloat);
                                        categoryName_in.add(category.getName());
                                        totalAmount_in = totalAmount_in + sumAmountFloat;
                                        List<PieEntry> pieEntries = new ArrayList<>();
                                        for (int i=0; i<sumAmount_in.size(); i++){
                                            pieEntries.add(new PieEntry(sumAmount_in.get(i) / totalAmount_in * 100, categoryName_in.get(i)));
                                        }
                                        PieDataSet pieDataSet = new PieDataSet(pieEntries, "类别");
                                        pieDataSet.setColors(colors);
                                        PieData pieData = new PieData(pieDataSet);
                                        pieData.setDrawValues(true);
                                        pieData.setValueFormatter(new PercentFormatter());
                                        pieData.setValueTextSize(12f);
                                        pieChart_income.setData(pieData);
                                        pieChart_income.invalidate();
                                    }
                                });
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }else{
                        Toast.makeText(getActivity(),"查询结果为空。",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Log.i("smile", "错误码："+e.getErrorCode()+"，错误描述："+e.getMessage());
                }
            }
        }, BmobUser.getCurrentUser().getObjectId());

        //初始化支出饼图
        categoryName_out = new ArrayList<>();
        sumAmount_out = new ArrayList<>();
        totalAmount_out = 0;
        pieChart_outcome = getActivity().findViewById(R.id.pieChart_outcome);
        pieChart_outcome.setNoDataText("你竟然没有任何支出！");
        pieChart_outcome.setDrawCenterText(true);  //饼状图中间可以添加文字
        pieChart_outcome.setDrawHoleEnabled(true);//饼状图中心圆是否存在
        pieChart_outcome.setCenterText("我的支出");  //饼状图中间的文字
        Description description2 = new Description();
        description2.setText("支出类别统计");
        pieChart_outcome.setDescription(description2);
        String sql2 = "select sum(amount) from Bill where userId = ? and type = 'out' group by categoryId order by -sum(amount)";
        new BmobQuery<Bill>().doStatisticQuery(sql2,new QueryListener<JSONArray>(){
            @Override
            public void done(JSONArray jsonArray, BmobException e) {
                if(e ==null){
                    if(jsonArray != null){
                        if (categoryName_out.size() != 0)
                            categoryName_out.clear();
                        if (sumAmount_out.size() != 0)
                            sumAmount_out.clear();
                        for (int i=0; i<jsonArray.length(); i++){
                            try {
                                final JSONObject value = (JSONObject) jsonArray.get(i);
                                final String categoryId = ((JSONObject)value.get("categoryId")).getString("objectId");
                                final Float sumAmountFloat = Float.parseFloat(value.getString("_sumAmount"));
                                new BmobQuery<Category>().getObject(categoryId, new QueryListener<Category>() {
                                    @Override
                                    public void done(Category category, BmobException e) {
                                        sumAmount_out.add(sumAmountFloat);
                                        categoryName_out.add(category.getName());
                                        totalAmount_out = totalAmount_out + sumAmountFloat;
                                        List<PieEntry> pieEntries = new ArrayList<>();
                                        for (int i=0; i<sumAmount_out.size(); i++){
                                            pieEntries.add(new PieEntry(sumAmount_out.get(i) / totalAmount_out * 100, categoryName_out.get(i)));
                                        }
                                        PieDataSet pieDataSet = new PieDataSet(pieEntries, "类别");
                                        pieDataSet.setColors(colors);
                                        PieData pieData = new PieData(pieDataSet);
                                        pieData.setDrawValues(true);
                                        pieData.setValueFormatter(new PercentFormatter());
                                        pieData.setValueTextSize(12f);
                                        pieChart_outcome.setData(pieData);
                                        pieChart_outcome.invalidate();
                                    }
                                });
                            } catch (JSONException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }else{
                        Toast.makeText(getActivity(),"查询结果为空。",Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Log.i("smile", "错误码："+e.getErrorCode()+"，错误描述："+e.getMessage());
                }
            }
        }, BmobUser.getCurrentUser().getObjectId());


    }

    /**
     * 分析关键词与给出建议
     */
    private void AnalyzeKeyword() {
        text_keyword = getActivity().findViewById(R.id.text_keyword);
        text_keyword_comment = getActivity().findViewById(R.id.text_keyword_comment);
        text_suggestion = getActivity().findViewById(R.id.textView_suggestion);

        if (totalOutcome / totalIncome > 1){
            text_keyword.setText("啃存款一族");
            text_keyword_comment.setText("你花的比赚的还多。这样下去养老钱会没的。");
            if (text_suggestion.getText().equals("继续记账，让我们能够给你建议！"))
                text_suggestion.setText("");
            text_suggestion.setText(text_suggestion.getText() + "\n" + "你的花销习惯非常不良，一直在消耗你的存款，这样的日子总有一天会到头的。建议你立即作出消费计划，精简支出。");
        }
        if (totalOutcome / totalIncome < 1 && totalOutcome / totalIncome > 0.8){
            text_keyword.setText("月光族");
            text_keyword_comment.setText("钱是我的，不花白不花。");
            if (text_suggestion.getText().equals("继续记账，让我们能够给你建议！"))
                text_suggestion.setText("");
            text_suggestion.setText(text_suggestion.getText() + "\n" + "你对你的收入心里很有数，每次都将其花得一干二净。但不要忘记存一些救命钱。");
        }
        if (totalOutcome / totalIncome < 0.5){
            text_keyword.setText("储蓄爱好者");
            text_keyword_comment.setText("你不喜欢花钱，喜欢把赚的都留着。");
            if (text_suggestion.getText().equals("继续记账，让我们能够给你建议！"))
                text_suggestion.setText("");
            text_suggestion.setText(text_suggestion.getText() + "\n" + "你把你赚的钱大多数都存起来了，是好是坏，看你自己怎么想。");
        }
        if (engelCoefficient > 0.5){
            text_keyword.setText("为下一餐拼搏");
            text_keyword_comment.setText("你一直在为下一餐能吃上饭而努力。辛苦了，奋斗者。");
            if (text_suggestion.getText().equals("继续记账，让我们能够给你建议！"))
                text_suggestion.setText("");
            text_suggestion.setText(text_suggestion.getText() + "\n" + "很不幸，你还是社会中的底层群众。请一定要继续努力，获得更多的收入。");
        }
        if (engelCoefficient < 0.3){
            text_keyword.setText("小康人士");
            text_keyword_comment.setText("小康社会，你已经身在其中。");
            if (text_suggestion.getText().equals("继续记账，让我们能够给你建议！"))
                text_suggestion.setText("");
            text_suggestion.setText(text_suggestion.getText() + "\n" + "你的条件比较好，能够做很多自己喜欢的事情。保持下去！");
        }
    }

    //获取总收入或支出
    private void getTotalIncomeOrOutcome(final String inOrOut){
        Calendar calendar = Calendar.getInstance();

        BmobQuery<Bill> bmobQuery = new BmobQuery<>();
        bmobQuery.addWhereEqualTo("userId", BmobUser.getCurrentUser().getObjectId());
        if (inOrOut.equals("in"))
            bmobQuery.addWhereEqualTo("type", "in");
        else if (inOrOut.equals("out"))
            bmobQuery.addWhereEqualTo("type", "out");
        bmobQuery.addWhereGreaterThan("date", new BmobDate(new Date(calendar.get(Calendar.YEAR-1), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        bmobQuery.findObjects(new FindListener<Bill>() {
            @Override
            public void done(List<Bill> list, BmobException e) {
                if (e == null){
                    float sum = 0;
                    for (Bill bill: list){
                        sum = sum + bill.getAmount();
                    }
                    if (inOrOut.equals("in")){
                        text_income.setText("￥" + sum);
                        totalIncome = sum;
                    }
                    if (inOrOut.equals("out")){
                        text_outcome.setText("￥" + sum);
                        totalOutcome = sum;
                    }
                }
                else
                    Toast.makeText(getActivity(),"查询账单出错:" + e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    //获取恩格尔系数
    private void getEngelCoefficient(){
        BmobQuery<Category> categoryBmobQuery = new BmobQuery<>();
        categoryBmobQuery.addWhereEqualTo("userId", BmobUser.getCurrentUser().getObjectId());
        categoryBmobQuery.addWhereEqualTo("name", "消费-饮食");
        categoryBmobQuery.findObjects(new FindListener<Category>() {
            @Override
            public void done(List<Category> list, BmobException e) {
                if (e == null){
                    if (list.size() == 1){
                        String categoryId = list.get(0).getObjectId();
                        getAndSetEngelCoefficient(categoryId);
                    }
                    else
                        Toast.makeText(getActivity(),"无法统计恩格尔系数。你删除或重复新增了“消费-饮食”类别。",Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(getActivity(),"查询账单出错:" + e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 从数据库获取恩格尔系数
     * @param categoryId 该用户的饮食类别Id
     */
    private void getAndSetEngelCoefficient(String categoryId) {
        Calendar calendar = Calendar.getInstance();
        BmobQuery<Bill> billBmobQuery = new BmobQuery<>();
        billBmobQuery.addWhereEqualTo("userId", BmobUser.getCurrentUser().getObjectId());
        billBmobQuery.addWhereEqualTo("type", "out");
        billBmobQuery.addWhereEqualTo("categoryId", new Category(categoryId));
        billBmobQuery.addWhereGreaterThan("date", new BmobDate(new Date(calendar.get(Calendar.YEAR-1), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))));
        billBmobQuery.findObjects(new FindListener<Bill>() {
            @Override
            public void done(List<Bill> list, BmobException e) {
                if (e == null){
                    float sum = 0;
                    for (Bill bill: list){
                        sum = sum + bill.getAmount();
                    }
                    float totalOutcome = Float.parseFloat(text_outcome.getText().toString().substring(1));
                    text_EngelCoefficient.setText((sum / totalOutcome * 100) + "%");
                    engelCoefficient = sum / totalOutcome;

                    AnalyzeKeyword();
                }
                else
                    Toast.makeText(getActivity(),"查询账单出错:" + e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

}
