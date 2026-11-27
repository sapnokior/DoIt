import sqlite3
import datetime
import os

db_path = r'C:\Users\adars\.gemini\antigravity\brain\37bcaa86-af47-463f-aaf1-6c9d9bd81435\schedules_raw.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
tables = [t[0] for t in cursor.fetchall()]
print("Tables:", tables)

for table in tables:
    if table.startswith("android_") or table.startswith("room_"):
        continue
    print(f"\n--- Table: {table} ---")
    cursor.execute(f"PRAGMA table_info({table});")
    cols = [c[1] for c in cursor.fetchall()]
    print("Columns:", cols)
    cursor.execute(f"SELECT * FROM {table} LIMIT 20;")
    rows = cursor.fetchall()
    for r in rows:
        print(r)
