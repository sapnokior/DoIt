"""
Real-time Train Tracker for 12843 (Puri - Ahmedabad SF Express)
Updates live status and upcoming station details every minute.
"""

import os
import sys
import time
from datetime import datetime
from ntes import NTESClient
from ntes.exceptions import NTESError

TRAIN_NO = "12843"
UPDATE_INTERVAL_SECONDS = 60


def clear_console():
    """Clears the terminal screen for a clean dashboard view."""
    os.system("cls" if os.name == "nt" else "clear")


def format_delay(delay_val):
    if not delay_val or delay_val == "0" or delay_val == "On Time":
        return "On Time (Right Time)"
    try:
        mins = int(delay_val)
        return f"Late by {mins} mins"
    except (ValueError, TypeError):
        return f"{delay_val}"


def fetch_and_display(client: NTESClient, start_date: str):
    now_str = datetime.now().strftime("%I:%M:%S %p | %d-%b-%Y")
    
    try:
        status = client.live_status(TRAIN_NO, start_date)
    except (NTESError, Exception) as e:
        print(f"[{now_str}] ⚠️ Connection error fetching live status: {e}")
        print(f"Retrying in {UPDATE_INTERVAL_SECONDS} seconds...")
        return

    if not status or "TNM" not in status:
        print(f"[{now_str}] ⚠️ Received incomplete data. Retrying in {UPDATE_INTERVAL_SECONDS}s...")
        return

    train_name = status.get("TNM", "PURI ADI S F")
    current_pos = status.get("CPOS", "N/A")
    last_updated = status.get("LUPDT", "N/A")
    current_delay = status.get("LDEL", "0")
    last_station_name = status.get("LSTNN", "N/A")
    last_station_code = status.get("LSTN", "")
    
    next_station_name = status.get("NPSTNN", "N/A")
    next_station_code = status.get("NPSTN", "")

    # Look up next station timing and platform in the station list
    stns = status.get("STNS", [])
    next_stn_data = next((s for s in stns if s.get("SC") == next_station_code), None)
    
    # Also find Vizianagaram (VZM) status for tracking partner's boarding
    vzm_data = next((s for s in stns if s.get("SC") == "VZM"), None)

    # Render Dashboard
    clear_console()
    print("=" * 70)
    print(f"       🚆 LIVE TRAIN TRACKER: {TRAIN_NO} - {train_name}")
    print(f"       🗓 Journey Date: {start_date}  |  🕒 As of: {now_str}")
    print("=" * 70)

    # 1. Current Live Status
    print("\n📍 CURRENT POSITION & RUNNING STATUS:")
    print(f"   • Status:       {current_pos}")
    print(f"   • Last Station: {last_station_name} ({last_station_code})")
    print(f"   • Delay:        {format_delay(current_delay)}")
    print(f"   • Telemetry at: {last_updated}")

    # 2. Next Coming Station
    print("\n⏭️ NEXT COMING STATION:")
    if next_stn_data:
        eta = next_stn_data.get("ETA", "N/A")
        sta = next_stn_data.get("STA", "N/A")
        etd = next_stn_data.get("ETD", "N/A")
        std = next_stn_data.get("STD", "N/A")
        pf = next_stn_data.get("PF") or "To be announced"
        arr_delay = next_stn_data.get("DARR", "On Time")
        dist = next_stn_data.get("DIST", "N/A")

        print(f"   • Station:      {next_station_name} ({next_station_code})")
        print(f"   • Platform:     {pf}")
        print(f"   • Est. Arrival: {eta}  (Sch: {sta})")
        print(f"   • Est. Depart:  {etd}  (Sch: {std})")
        print(f"   • Arr. Delay:   {format_delay(arr_delay)}")
        print(f"   • Distance:     {dist} km from PURI")
    else:
        print(f"   • Station:      {next_station_name} ({next_station_code})")
        print("   • Timings:      Details updating shortly...")

    # 3. Vizianagaram (VZM) Boarding Watch
    if vzm_data:
        vzm_eta = vzm_data.get("ETA", "23:55 24-Nov")
        vzm_etd = vzm_data.get("ETD", "00:20 25-Nov")
        vzm_pf = vzm_data.get("PF", "1")
        vzm_delay = vzm_data.get("DARR", "On Time")
        vzm_is_departed = vzm_data.get("ISD", False)
        
        status_tag = "DEPARTED ✓" if vzm_is_departed else "UPCOMING BOARDING"
        print("\n🎫 BOARDING STATION WATCH: VIZIANAGARAM JN (VZM)")
        print(f"   • Status:       {status_tag}")
        print(f"   • Platform:     Platform {vzm_pf}  (25-min Engine Reversal Halt)")
        print(f"   • Est. Arrival: {vzm_eta}")
        print(f"   • Est. Depart:  {vzm_etd}")
        print(f"   • Est. Delay:   {format_delay(vzm_delay)}")

    print("\n" + "-" * 70)
    print(f"🔄 Auto-updating every {UPDATE_INTERVAL_SECONDS} seconds. Press Ctrl + C to stop.")
    print("-" * 70)


def main():
    # Use today's date in DD-Mon-YYYY format (e.g. 24-Nov-2026)
    start_date = datetime.now().strftime("%d-%b-%Y")
    client = NTESClient(timeout=10, retries=2)

    print(f"Starting real-time tracking for train {TRAIN_NO} started on {start_date}...")
    time.sleep(1)

    try:
        while True:
            fetch_and_display(client, start_date)
            # Sleep in 1-second chunks so Ctrl+C responds immediately
            for _ in range(UPDATE_INTERVAL_SECONDS):
                time.sleep(1)
    except KeyboardInterrupt:
        print("\n\n🛑 Tracking stopped by user. Have a safe journey!")
        sys.exit(0)


if __name__ == "__main__":
    main()
