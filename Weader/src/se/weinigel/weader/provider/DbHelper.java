package se.weinigel.weader.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
	public DbHelper(Context context) {
		super(context, DbSchema.DATABASE_NAME, null, DbSchema.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL(DbSchema.FeedSchema.CREATE_TABLE);
        db.execSQL(DbSchema.ItemSchema.CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public void hack() {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL("DROP VIEW feeds_unread");
		} catch (SQLiteException e) {
		}
		db.execSQL("CREATE VIEW feeds_unread AS"
				+ " SELECT feeds.*, COUNT(items._id) AS unread" + " FROM feeds"
				+ " OUTER LEFT JOIN items" + " ON feeds._id=items.feed_id"
				+ " AND items.read=0" + " GROUP BY feeds._id");
		db.close();
	}
}
