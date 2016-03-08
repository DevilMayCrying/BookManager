package com.example.zane.bookmanager.presenters.fragment;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.zane.bookmanager.R;
import com.example.zane.bookmanager.app.MyApplication;
import com.example.zane.bookmanager.inject.component.DaggerFragmentComponent;
import com.example.zane.bookmanager.inject.module.FragmentModule;
import com.example.zane.bookmanager.model.bean.Book_DB;
import com.example.zane.bookmanager.presenters.MainActivity;
import com.example.zane.bookmanager.presenters.activity.MyBookDetailInfoActivity;
import com.example.zane.bookmanager.presenters.activity.ZxingScannerActivity;
import com.example.zane.bookmanager.presenters.adapter.MyBookInfoAdapter;
import com.example.zane.bookmanager.utils.ChangeWindowsAlpha;
import com.example.zane.bookmanager.view.MyBookInfoView;
import com.example.zane.easymvp.presenter.BaseFragmentPresenter;
import com.kermit.exutils.utils.ExUtils;

import org.litepal.crud.DataSupport;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.Inflater;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Zane on 16/2/16.
 */
public class MyBookInfoFragment extends BaseFragmentPresenter<MyBookInfoView>{

    public static final String TAG = "MyBookInfoFragment";

    private LinearLayoutManager manager;
    private List<Book_DB> myBooks;
    private MyBookInfoAdapter adapter;
    public static final String BOOK_DB = "BOOK_DB";
   // private OnAddButtonListener scannerButtonListener;
    private boolean isSortByDate;
    //1_all, 2_name, 3_author
    private int checkByWhitch = 1;
    private String bookName = "";
    private Observable<Integer> observable;
    private Subscriber<Integer> subscriber;
    private RecyclerView recyclerView;



    public static MyBookInfoFragment newInstance(){
        MyBookInfoFragment fragment = new MyBookInfoFragment();
        return fragment;
    }


    @Override
    public Class<MyBookInfoView> getRootViewClass() {
        return MyBookInfoView.class;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchDataByDate();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
        initInject();
        fetchDataByDate();
        adapter.notifyDataSetChanged();
        v.initRecycleView(manager, adapter);
        v.setFabMenu();

        //搜索功能的接口实现
        adapter.setOnCheckBookListener(new MyBookInfoAdapter.OnCheckBookListener() {
            @Override
            public void onCheckBook(final String book_name) {
                if (!TextUtils.isEmpty(book_name)) {
                    bookName = book_name;
                    switch (checkByWhitch) {
                        case 1:
                            checkBookByAll(book_name);
                            break;
                        case 2:
                            checkBookByName(book_name);
                            break;
                        case 3:
                            checkBookByAuthor(book_name);
                            break;
                        default:
                            checkBookByAll(book_name);
                    }
                } else {
                    //如果输入框为空，那么这个也要为空
                    bookName = "";
                    if (isSortByDate) {
                        fetchDataByDate();
                        adapter.notifyDataSetChanged();
                    } else {
                        fetchFataByName();
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCheckChoose(View v) {
                showPopupView(v);
            }
        });

        //监听滑动，然后判断fab的显示与否
        recyclerView = v.get(R.id.recyclerview_mybookinfo_fragment);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0){
                    v.hideFab();
                }else if (dy < 0){
                    v.showFab();
                }
            }
        });
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v1) {
                getActivity().startActivityForResult(new Intent(getActivity()
                                                                       , ZxingScannerActivity.class), MainActivity.requestCode_1);
                v.closeMenu();
            }
        }, R.id.fab_scanner_mybookinfo_fragment);

        //判断哪种排序方式的接口实现
        adapter.setOnSortBookListener(new MyBookInfoAdapter.OnSortBookListener() {
            @Override
            public void onSortByName() {
                fetchFataByName();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onSortByDate() {
                fetchDataByDate();
                adapter.notifyDataSetChanged();
            }
        });
        //item的点击事件的监听
        adapter.setOnItemClickListener(new MyBookInfoAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                Intent intent = new Intent(getActivity(), MyBookDetailInfoActivity.class);
                intent.putExtra(MainActivity.TAG, MainActivity.TAG);
                intent.putExtra(BOOK_DB, myBooks.get(position));
                getActivity().startActivity(intent);
            }

            @Override
            public void onLongClick(final int positon) {

                new MaterialDialog.Builder(getActivity())
                        .title(R.string.choose)
                        .items(new CharSequence[]{"删除"})
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i
                                                           , CharSequence charSequence) {
                                switch (i) {
                                    case 0:
                                        DataSupport.deleteAll(Book_DB.class, "isbn13 = ?", myBooks.get(positon).getIsbn13());
                                        myBooks.remove(positon);
                                        adapter.clear();
                                        if (isSortByDate) {
                                            fetchDataByDate();
                                        } else {
                                            fetchFataByName();
                                        }
                                        adapter.notifyDataSetChanged();
                                        Toast.makeText(getActivity(), "已删除", Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            }
                        }).show();
            }
        });
    }

    //显示查询条目的pop
    public void showPopupView(View v){

        ChangeWindowsAlpha.changeWindowsAlpha(getActivity(), 0.7f);

        TextView checkAll;
        TextView checkName;
        TextView checkAuthor;

        View popView = LayoutInflater.from(MyApplication.getApplicationContext2())
                .inflate(R.layout.fragment_popupwindows_mybookinfo, null);

        checkAll = (TextView) popView.findViewById(R.id.check_all);
        checkName = (TextView) popView.findViewById(R.id.check_name);
        checkAuthor = (TextView) popView.findViewById(R.id.check_author);

        final PopupWindow popupWindow = new PopupWindow(popView, 350, LinearLayout.LayoutParams.WRAP_CONTENT);

        //popupWindow.showAsDropDown(v);必须放在最后，我日
        popupWindow.setOutsideTouchable(true);
        popView.setFocusableInTouchMode(true);
        popupWindow.setTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setAnimationStyle(R.style.Animation_AppCompat_DropDownUp);
        popupWindow.showAsDropDown(v);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                ChangeWindowsAlpha.changeWindowsAlpha(getActivity(), 1f);
            }
        });
        popupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            }
        });

        subscriber = new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                if(bookName != "") {
                    switch (integer) {
                        case 1:
                            checkBookByAll(bookName);
                            break;
                        case 2:
                            checkBookByName(bookName);
                            break;
                        case 3:
                            checkBookByAuthor(bookName);
                            break;
                        default:
                            checkBookByAll(bookName);
                    }
                }
            }
        };

        //通过rx实现响应式的界面（一旦选择之后，立即查询）
        checkAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkByWhitch = 1;
                observable = Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        subscriber.onNext(1);
                    }
                });
                observable.subscribe(subscriber);
                popupWindow.dismiss();
            }
        });
        checkName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkByWhitch = 2;
                observable = Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        subscriber.onNext(2);
                    }
                });
                observable.subscribe(subscriber);
                popupWindow.dismiss();
            }
        });
        checkAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkByWhitch = 3;
                observable = Observable.create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        subscriber.onNext(3);
                    }
                });
                observable.subscribe(subscriber);
                popupWindow.dismiss();
            }
        });
    }

    public void checkBookByAll(final String book_name){
        final Map<String, Book_DB> maps = new HashMap<>();
        final List<Book_DB> books = new ArrayList<>();
        for (int i = 0; i < book_name.length(); i++) {
            TextUtils.substring(book_name, 0, i + 1);
            final int I = i;
            Observable.from(myBooks)
                    .map(new Func1<Book_DB, String>() {
                        @Override
                        public String call(Book_DB book_db) {
                            maps.put(book_db.getTitle(), book_db);
                            return book_db.getTitle();
                        }
                    })
                    .filter(new Func1<String, Boolean>() {
                        @Override
                        public Boolean call(String s) {
                            String n = "";
                            for (int j = 0; j < s.length() - I; j++) {
                                n = TextUtils.substring(s, j, I + j + 1);
                                if (n.equals(book_name)) {
                                    break;
                                }
                            }
                            return n.equals(book_name);
                        }
                    })
                    .map(new Func1<String, Book_DB>() {
                        @Override
                        public Book_DB call(String s) {
                            return maps.get(s);
                        }
                    })
                    .subscribe(new Action1<Book_DB>() {
                        @Override
                        public void call(Book_DB book_db) {
                            books.add(book_db);
                        }
                    });

            Observable.from(myBooks)
                    .map(new Func1<Book_DB, String>() {
                        @Override
                        public String call(Book_DB book_db) {
                            maps.put(book_db.getAuthor(), book_db);
                            return book_db.getAuthor();
                        }
                    })
                    .filter(new Func1<String, Boolean>() {
                        @Override
                        public Boolean call(String s) {
                            String n = "";
                            for (int j = 0; j < s.length() - I; j++) {
                                n = TextUtils.substring(s, j, I + j + 1);
                                if (n.equals(book_name)) {
                                    break;
                                }
                            }
                            return n.equals(book_name);
                        }
                    })
                    .map(new Func1<String, Book_DB>() {
                        @Override
                        public Book_DB call(String s) {
                            return maps.get(s);
                        }
                    })
                    .subscribe(new Action1<Book_DB>() {
                        @Override
                        public void call(Book_DB book_db) {
                            books.add(book_db);
                        }
                    });

        }
        if (books.size() != 0) {
            myBooks = books;
            adapter.setMyBooks(myBooks);
            adapter.notifyDataSetChanged();
        } else {
            adapter.setMyBooks(new ArrayList<Book_DB>());
            adapter.notifyDataSetChanged();
        }
    }
    public void checkBookByAuthor(final String book_name){
        final Map<String, Book_DB> maps = new HashMap<>();
        final List<Book_DB> books = new ArrayList<>();
        for (int i = 0; i < book_name.length(); i++) {
            TextUtils.substring(book_name, 0, i + 1);
            final int I = i;
            Observable.from(myBooks)
                    .map(new Func1<Book_DB, String>() {
                        @Override
                        public String call(Book_DB book_db) {
                            maps.put(book_db.getAuthor(), book_db);
                            return book_db.getAuthor();
                        }
                    })
                    .filter(new Func1<String, Boolean>() {
                        @Override
                        public Boolean call(String s) {
                            String n = "";
                            for (int j = 0; j < s.length() - I; j++) {
                                n = TextUtils.substring(s, j, I + j + 1);
                                if (n.equals(book_name)) {
                                    break;
                                }
                            }
                            return n.equals(book_name);
                        }
                    })
                    .map(new Func1<String, Book_DB>() {
                        @Override
                        public Book_DB call(String s) {
                            return maps.get(s);
                        }
                    })
                    .subscribe(new Action1<Book_DB>() {
                        @Override
                        public void call(Book_DB book_db) {
                            books.add(book_db);
                        }
                    });

        }
        if (books.size() != 0) {
            myBooks = books;
            adapter.setMyBooks(myBooks);
            adapter.notifyDataSetChanged();
        } else {
            adapter.setMyBooks(new ArrayList<Book_DB>());
            adapter.notifyDataSetChanged();
        }
    }

    public void checkBookByName(final String book_name){
        final Map<String, Book_DB> maps = new HashMap<>();
        final List<Book_DB> books = new ArrayList<>();
        for (int i = 0; i < book_name.length(); i++) {
            TextUtils.substring(book_name, 0, i + 1);
            final int I = i;
            Observable.from(myBooks)
                    .map(new Func1<Book_DB, String>() {
                        @Override
                        public String call(Book_DB book_db) {
                            maps.put(book_db.getTitle(), book_db);
                            return book_db.getTitle();
                        }
                    })
                    .filter(new Func1<String, Boolean>() {
                        @Override
                        public Boolean call(String s) {
                            String n = "";
                            for (int j = 0; j < s.length() - I; j++) {
                                n = TextUtils.substring(s, j, I + j + 1);
                                if (n.equals(book_name)) {
                                    break;
                                }
                            }
                            return n.equals(book_name);
                        }
                    })
                    .map(new Func1<String, Book_DB>() {
                        @Override
                        public Book_DB call(String s) {
                            return maps.get(s);
                        }
                    })
                    .subscribe(new Action1<Book_DB>() {
                        @Override
                        public void call(Book_DB book_db) {
                            books.add(book_db);
                        }
                    });
        }
        if (books.size() != 0) {
            myBooks = books;
            adapter.setMyBooks(myBooks);
            adapter.notifyDataSetChanged();
        } else {
            adapter.setMyBooks(new ArrayList<Book_DB>());
            adapter.notifyDataSetChanged();
        }
    }

    public void init(){
        manager = new LinearLayoutManager(MyApplication.getApplicationContext2());
        adapter = new MyBookInfoAdapter(MyApplication.getApplicationContext2());
    }
    public void initInject(){
        MainActivity activity = (MainActivity)getActivity();
        DaggerFragmentComponent.builder()
                .fragmentModule(new FragmentModule())
                .activityComponent(activity.getActivityComponent())
                .build()
                .inject(this);
    }

    //现在只是在数据库中查询哈
    //根据扫描时间降序排列
    public void fetchDataByDate(){
        myBooks = DataSupport.order("id desc").find(Book_DB.class);
        adapter.setMyBooks(myBooks);
        isSortByDate = true;
    }
    //根据书名开头第一个汉字拼音升序排列
    public void fetchFataByName(){
        Map<String, Book_DB> maps = new HashMap<>();
        List<String> bookName = new ArrayList<>();
        List<Book_DB> books = new ArrayList<>();

        myBooks = DataSupport.findAll(Book_DB.class);

        for (Book_DB book : myBooks) {
            maps.put(book.getTitle(), book);
            bookName.add(book.getTitle());
        }

        Collections.sort(bookName, Collator.getInstance(Locale.CHINESE));

        for (int i = 0; i < myBooks.size(); i++){
            books.add(maps.get(bookName.get(i)));
        }
        myBooks = books;
        //myBooks = DataSupport.order("title asc").find(Book_DB.class);
        adapter.setMyBooks(myBooks);
        isSortByDate = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }
}
