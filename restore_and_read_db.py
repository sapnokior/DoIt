import base64
import sqlite3
import datetime

b64_db_path = r'C:\Users\adars\.gemini\antigravity\brain\37bcaa86-af47-463f-aaf1-6c9d9bd81435\db_b64.txt'
out_db_path = r'C:\Users\adars\.gemini\antigravity\brain\37bcaa86-af47-463f-aaf1-6c9d9bd81435\schedules_restored.db'

with open(b64_db_path, 'r') as f:
    data = base64.b64decode(f.read().strip())

with open(out_db_path, 'wb') as f:
    f.write(data)

b64_wal_path = r'C:\Users\adars\.gemini\antigravity\brain\37bcaa86-af47-463f-aaf1-6c9d9bd81435\wal_b64.txt'
out_wal_path = r'C:\Users\adars\.gemini\antigravity\brain\37bcaa86-af47-463f-aaf1-6c9d9bd81435\schedules_restored.db-wal'

try:
    with open(b64_wal_path, 'r') as f:
        wal_data = base64.b64decode(f.read().strip())
    with open(out_wal_path, 'wb') as f:
        f.write(wal_data)
except Exception as e:
    print('No wal:', e)

conn = sqlite3.connect(out_db_path)
c = conn.cursor()
c.execute("SELECT name FROM sqlite_master WHERE type='table';")
print("Tables:", c.fetchall())

# Query templates or time tasks
for tbl in ['templates', 'time_tasks', 'schedules', 'undefined_tasks']:
    try:
        c.execute(f"SELECT * FROM {tbl};")
        rows = c.fetchall()
        print(f"\n=== Table: {tbl} ({len(rows)} rows) ===")
        c.execute(f"PRAGMA table_info({tbl});")
        col_names = [col[1] for col in c.fetchall()]
        print("Columns:", col_names)
        for r in rows:
            print(dict(zip(col_names, r)))
    except Exception as e:
        print(f"Error reading {tbl}: {e}")
