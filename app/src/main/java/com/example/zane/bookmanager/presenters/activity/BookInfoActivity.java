package com.example.zane.bookmanager.presenters.activity;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.example.zane.bookmanager.R;
import com.example.zane.bookmanager.app.MyApplication;
import com.example.zane.bookmanager.inject.component.DaggerActivityComponent;
import com.example.zane.bookmanager.inject.module.ActivityModule;
import com.example.zane.bookmanager.inject.qualifier.ContextType;
import com.example.zane.bookmanager.model.bean.Book;
import com.example.zane.bookmanager.model.bean.Book_DB;
import com.example.zane.bookmanager.presenters.MainActivity;
import com.example.zane.bookmanager.view.BookInfoView;
import com.example.zane.easymvp.presenter.BaseActivityPresenter;
import com.kermit.exutils.utils.ExUtils;

import org.litepal.tablemanager.Connector;

import rx.Observable;
import rx.functions.Action1;

/**
 * Created by Zane on 16/2/14.
 * 扫描出来的书的结果显示页面
 */
public class BookInfoActivity extends BaseActivityPresenter<BookInfoView>{

    private Book book;
    private Toolbar toolbar;


    @Override
    public Class<BookInfoView> getRootViewClass() {
        return BookInfoView.class;
    }

    @Override
    public void inCreat(Bundle bundle) {

        book = (Book)getIntent().getSerializableExtra(MainActivity.BOOK_INFO);
        initInject();

        toolbar = v.get(R.id.toolbar_bookinfo_activity);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        v.setupFabMenu();
        v.setupNestScrollView();

        v.setupToolbar(book.getImages().getLarge());
        v.setBookInfo(book.getTitle(), book.getAuthor(), book.getPublisher(), book.getPrice());

        //存储图书数据到数据库,并且保证现在是从mainactivity跳转而来
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Book_DB book_db = new Book_DB();

                    final StringBuilder builder = new StringBuilder();
                    Observable.from(book.getAuthor())
                            .subscribe(new Action1<String>() {
                                @Override
                                public void call(String s) {
                                    builder.append(s).append(". ");
                                }
                            });

                    book_db.setAuthor(builder.toString());
                    book_db.setAuthor_intro(book.getAuthor_intro());
                    book_db.setImage(book.getImages().getLarge());
                    book_db.setPages(book.getPages());
                    book_db.setPrice(book.getPrice());
                    book_db.setPubdate(book.getPubdate());
                    book_db.setSubtitle(book.getSubtitle());
                    book_db.setSummary(book.getSummary());
                    book_db.setTitle(book.getTitle());
                    book_db.setUrl(book.getUrl());
                    book_db.setPublisher(book.getPublisher());
                    book_db.setIsbn13(book.getIsbn13());
                    if (book.getTags().size() >= 3) {
                        book_db.setTag1(book.getTags().get(0).getName());
                        book_db.setTag2(book.getTags().get(1).getName());
                        book_db.setTag3(book.getTags().get(2).getName());
                    } else {
                        switch (book.getTags().size()) {
                            case 0:
                                break;
                            case 1:
                                book_db.setTag1(book.getTags().get(0).getName());
                                break;
                            case 2:
                                book_db.setTag1(book.getTags().get(0).getName());
                                book_db.setTag2(book.getTags().get(1).getName());
                                break;
                        }
                    }

                    if (book_db.save()) {
                        ExUtils.Toast("保存成功!");
                        finish();
                    } else {
                        ExUtils.Toast("保存失败!");
                        finish();
                    }
                }
            }, R.id.fab_add_bookinfo_fragment);

    }

    public void initInject(){
        MyApplication app = MyApplication.getApplicationContext2();
        DaggerActivityComponent.builder()
                .activityModule(new ActivityModule(this))
                .applicationComponent(app.getAppComponent())
                .build()
                .inject(this);
    }

    @Override
    public void inDestory() {

    }
}
