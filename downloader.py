
import os
import json
import urllib.request
import urllib.error

def get_file_info():
    files_json_url = "https://samp-mobile.shop/files.json"
    try:
        with urllib.request.urlopen(files_json_url) as response:
            data = json.loads(response.read().decode())
            return data.get("files", [])
    except Exception as e:
        print(f"Error fetching file list: {e}")
        return []

def download_file(url, target_path):
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    print(f"Downloading {url} to {target_path}...")
    try:
        urllib.request.urlretrieve(url, target_path)
        print(f"Finished {target_path}")
    except Exception as e:
        print(f"Failed to download {url}: {e}")

def main():
    base_url = "https://samp-mobile.shop/files/"
    files = get_file_info()
    
    if not files:
        print("No files found or error fetching list.")
        return

    total_size = sum(int(f.get("size", 0)) for f in files)
    total_mb = total_size / (1024 * 1024)
    total_gb = total_mb / 1024
    
    print(f"Found {len(files)} files.")
    print(f"Total size: {total_size} bytes ({total_mb:.2f} MB / {total_gb:.2f} GB)")
    
    # Due to environment limitations, we might not be able to download GBs of data in one turn.
    # I will download the small files first or just start the process.
    # Given the request, I'll try to download everything but I should probably warn if it's huge.
    
    for i, file_info in enumerate(files):
        file_path = file_info.get("path")
        target_path = os.path.join("gamedata", file_path)
        download_url = base_url + file_path.replace(" ", "%20") # Handle spaces in URLs
        
        expected_size = int(file_info.get("size", 0))
        
        if os.path.exists(target_path):
            if os.path.getsize(target_path) == expected_size:
                print(f"Skipping {target_path}, already complete.")
                continue
            
        # Optional: Skip very large files if they exceed a certain threshold to avoid timeout
        # if expected_size > 100 * 1024 * 1024: # 100MB
        #     print(f"Skipping large file {file_path} ({expected_size} bytes) to avoid timeout.")
        #     continue

        download_file(download_url, target_path)

if __name__ == "__main__":
    main()
