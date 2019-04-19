package com.truthower.suhang.mangareader.business.statistics;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.truthower.suhang.mangareader.R;
import com.truthower.suhang.mangareader.base.BaseMultiTabActivity;


/**
 * 资金记录页
 */
public class StatisticsActivity extends BaseMultiTabActivity {
    private CalendarStatisticsFragment calendarListFragment;
    private BookStatisticsFragment bookBreaklineFragment;
    private TabLayout tabLayout;
    private ViewPager vp;
    private MyFragmentPagerAdapter adapter;
    private String[] pageTitle = new String[]{"以日期为单位", "以漫画为单位"};

    @Override
    protected void initFragment() {
        calendarListFragment = new CalendarStatisticsFragment();
        bookBreaklineFragment = new BookStatisticsFragment();
    }

    @Override
    protected String getActivityTitle() {
        return "统计数据";
    }

    @Override
    protected int getPageCount() {
        return 2;
    }

    @Override
    protected String[] getTabTitleList() {
        return pageTitle;
    }

    @Override
    protected Fragment getFragmentByPosition(int position) {
        switch (position) {
            case 0:
                return calendarListFragment;
            case 1:
                return bookBreaklineFragment;
            default:
                return null;
        }
    }
}
