package mchenys.net.csdn.blog.dragdelete;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private GridView mGridView;
    private LinearLayout mDeleteLayout;
    private View mDragView;
    private int mDownX;
    private int mDownY;

    /**
     * 震动器
     */
    private Vibrator mVibrator;

    private WindowManager mWindowManager;
    /**
     * item镜像的布局参数
     */
    private WindowManager.LayoutParams mWindowLayoutParams;

    /**
     * 我们拖拽的item对应的Bitmap
     */
    private Bitmap mDragBitmap;

    private int mStatusHeight;
    private ImageView mDragImageView;
    /**
     * 距离屏幕顶部的偏移量
     */
    private int mOffset2Top;
    /**
     * 距离屏幕左边的偏移量
     */
    private int mOffset2Left;
    /**
     * 按下的点到所在item的上边缘的距离
     */
    private int mPoint2ItemTop;
    /**
     * 按下的点到所在item的左边缘的距离
     */
    private int mPoint2ItemLeft;
    private int mDragPosition;
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGridView = (GridView) findViewById(R.id.gv);
        mDeleteLayout = (LinearLayout) findViewById(R.id.ll_delete);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mStatusHeight = getStatusHeight(this); //获取状态栏的高度

        ArrayList<String> data = new ArrayList<>();
        for (int i = 'a'; i < 'z'; i++) {
            data.add(String.valueOf((char) i));
        }
        mAdapter = new MyAdapter(data);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "position:" + position, Toast.LENGTH_SHORT).show();
                mVibrator.vibrate(50); //震动一下
                mDragPosition = position;
                mDragView = view;
                mDragView.setVisibility(View.INVISIBLE);
                //获取按下的点到所在的DragView的左边缘和上边缘的距离
                mPoint2ItemLeft = mDownX - mDragView.getLeft();
                mPoint2ItemTop = mDownY - mDragView.getTop();

                //开启mDragItemView绘图缓存
                mDragView.setDrawingCacheEnabled(true);
                //获取mDragItemView在缓存中的Bitmap对象
                mDragBitmap = Bitmap.createBitmap(mDragView.getDrawingCache());
                //这一步很关键，释放绘图缓存，避免出现重复的镜像
                mDragView.destroyDrawingCache();

                //根据我们按下的点显示item镜像
                createDragImage(mDragBitmap);
                return true;
            }
        });
        mGridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                Log.d(TAG, "mGridView->RY:" + ev.getRawY() + " Y:" + ev.getY() + " RX" + ev.getRawX() + " X:" + ev.getX());

                switch (ev.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //获取mDragView相对父控件按下时的坐标
                        mDownX = (int) ev.getX();
                        mDownY = (int) ev.getY();
                        //获取父控件距离屏幕的上边和左边的距离
                        mOffset2Top = (int) (ev.getRawY() - mDownY);
                        mOffset2Left = (int) (ev.getRawX() - mDownX);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (null != mDragView) {
                            int moveX = (int) ev.getX();
                            int moveY = (int) ev.getY();
                            mWindowLayoutParams.x = mOffset2Left + moveX - mPoint2ItemLeft;
                            mWindowLayoutParams.y = mOffset2Top + moveY - mPoint2ItemTop - mStatusHeight;
                            //限制滑动范围
                            if (mWindowLayoutParams.y < mOffset2Top + mDownY - mPoint2ItemTop - mStatusHeight) {
                                mWindowLayoutParams.y = mOffset2Top + mDownY - mPoint2ItemTop - mStatusHeight;
                            } else if (mWindowLayoutParams.y > mDeleteLayout.getBottom() + mStatusHeight) {
                                mWindowLayoutParams.y = mDeleteLayout.getBottom() + mStatusHeight;
                            }
                            Log.d(TAG, "x:" + mWindowLayoutParams.x + " y:" + mWindowLayoutParams.y);
                            mWindowManager.updateViewLayout(mDragImageView, mWindowLayoutParams); //更新镜像
                        }

                        break;
                    case MotionEvent.ACTION_UP:
                        removeItem();
                        removeDragImage();
                        break;
                }
                return false;
            }
        });
    }

    /**
     *创建移动的镜像
     * @param dragBitmap
     */
    private void createDragImage(Bitmap dragBitmap) {
        mWindowLayoutParams = new WindowManager.LayoutParams();
        mWindowLayoutParams.format = PixelFormat.TRANSLUCENT; //图片之外的其他地方透明
        mWindowLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWindowLayoutParams.x = mOffset2Left + mDownX - mPoint2ItemLeft;
        mWindowLayoutParams.y = mOffset2Top + mDownY - mPoint2ItemTop - mStatusHeight;
        mWindowLayoutParams.alpha = 0.55f; //透明度
        mWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        mDragImageView = new ImageView(this);
        mDragImageView.setImageBitmap(dragBitmap);
        mWindowManager.addView(mDragImageView, mWindowLayoutParams);
    }

    /**
     * .从界面上面移动拖动镜像
     * .
     */
    private void removeDragImage() {
        if (mDragImageView != null) {
            mWindowManager.removeView(mDragImageView);
            mDragImageView = null;
        }
    }
    private void removeItem() {
        if (null != mDragView) {
            RectF rectF = new RectF(mDeleteLayout.getLeft(), mDeleteLayout.getTop(), mDeleteLayout.getRight(), mDeleteLayout.getBottom()+mStatusHeight+1);
            if (rectF.contains(mWindowLayoutParams.x, mWindowLayoutParams.y)) {
                mAdapter.notifyItemDelete(mDragPosition);
            }
        }
    }


    class MyAdapter extends BaseAdapter {
        private List<String> data;

        public MyAdapter(List<String> data) {
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
//            ViewHolder holder = null;
//            if (null == convertView) {
//                convertView = View.inflate(getBaseContext(), R.layout.item_gv, null);
//                holder = new ViewHolder();
//                holder.infoTv = (TextView) convertView.findViewById(R.id.tv_info);
//                convertView.setTag(holder);
//            } else {
//                holder = (ViewHolder) convertView.getTag();
//            }
//            holder.infoTv.setText(getItem(position));

            convertView = View.inflate(getBaseContext(), R.layout.item_gv, null);
            TextView infoTv = (TextView) convertView.findViewById(R.id.tv_info);
            infoTv.setText(getItem(position));
            return convertView;
        }

        public void notifyItemDelete(int position) {
            data.remove(position);
            notifyDataSetChanged();
        }

        class ViewHolder {
            TextView infoTv;
        }
    }

    /**
     * 获取状态栏的高度
     *
     * @param context
     * @return
     */
    private static int getStatusHeight(Context context) {
        int statusHeight = 0;
        Rect localRect = new Rect();
        ((Activity) context).getWindow().getDecorView().getWindowVisibleDisplayFrame(localRect);
        statusHeight = localRect.top;
        if (0 == statusHeight) {
            Class<?> localClass;
            try {
                localClass = Class.forName("com.android.internal.R$dimen");
                Object localObject = localClass.newInstance();
                int i5 = Integer.parseInt(localClass.getField("status_bar_height").get(localObject).toString());
                statusHeight = context.getResources().getDimensionPixelSize(i5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return statusHeight;
    }
}
