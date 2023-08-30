package com.csctracker.androidtracker.service.monitor.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import com.csctracker.androidtracker.service.monitor.core.model.Erro;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SqlLitle extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "timetracker-desktop-plugin.db";

    public SqlLitle(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void salva(String json, String endpoint) {
        SQLiteDatabase db = this.getReadableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("json", json);
        if (DATABASE_VERSION >= 7) {
            contentValues.put("endpoint", endpoint);
        }
        db.insert("error", null, contentValues);

    }

    public void salvaLastSync(Long l) {
        SQLiteDatabase db = this.getReadableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("last_sync", l);
        db.insert("last_sync", null, contentValues);

    }

    public Long getLastSync() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.query("last_sync", new String[]{"MAX(last_sync.last_sync)"}, null, null, null, null, null);
        Long l = null;
        if (c.getCount() > 0) {
            c.moveToFirst();
            l = c.getLong(0);
        }
        c.close();
        db.close();
        return l;
    }

    public void getErrors(ConcurrentLinkedQueue<Erro> errors) throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from error", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            Erro e = new Erro();
            e.setJson(res.getString(res.getColumnIndex("json")));
            if (DATABASE_VERSION >= 7) {
                e.setEndpoint(res.getString(res.getColumnIndex("endpoint")));
            }
            errors.add(e);
            res.moveToNext();
        }
        res.close();
        db.execSQL("delete from error");
    }

    public void limpaSync() throws SQLException {
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("delete from last_sync");
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE error( json text, endpoint text)");
        sqLiteDatabase.execSQL("CREATE TABLE last_sync( last_sync integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        if (i == 6 && i1 == 7) {
            sqLiteDatabase.execSQL("ALTER TABLE error ADD COLUMN endpoint text");
        }
    }
}
