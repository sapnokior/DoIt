import sqlite3

f = r'C:\Users\adars\.gemini\antigravity\brain\37bcaa86-af47-463f-aaf1-6c9d9bd81435\schedules_exec.db'
conn = sqlite3.connect(f)
c = conn.cursor()
c.execute("SELECT name FROM sqlite_master WHERE type='table';")
print('Tables:', c.fetchall())
