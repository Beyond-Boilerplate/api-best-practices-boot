import requests
import json
import random
import time
import string
import argparse

# Function to generate fake data
def generate_fake_data():
    from_account = ''.join(random.choices(string.ascii_lowercase, k=8))
    to_account = ''.join(random.choices(string.ascii_lowercase, k=8))
    amount = round(random.uniform(50, 500), 2)
    return {
        "fromAccount": from_account,
        "toAccount": to_account,
        "amount": amount,
        "status": "ON_HOLD"
    }

# Function to call the API
def call_api(api_url, headers):
    data = generate_fake_data()
    response = requests.post(api_url, headers=headers, data=json.dumps(data))
    if response.status_code == 201:
        print(f"Transaction successful: {response.json()}")
    else:
        print(f"Error: {response.status_code} - {response.text}")

# Main function to handle configurable parameters
def main(api_url, frequency, content_type):
    headers = {'Content-Type': content_type}
    try:
        while True:
            call_api(api_url, headers)
            time.sleep(frequency)
    except KeyboardInterrupt:
        print("Script stopped by user.")

# Argument parser for configurable parameters
if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="API Transaction Caller")
    parser.add_argument('--url', type=str, default='http://localhost:8080/api/transactions', help='API URL to call')
    parser.add_argument('--frequency', type=int, default=10, help='Time interval between API calls in seconds')
    parser.add_argument('--content-type', type=str, default='application/json', help='Content-Type of the request')

    args = parser.parse_args()
    main(args.url, args.frequency, args.content_type)
