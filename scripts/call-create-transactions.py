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
        return True, None  # Successful call
    elif response.status_code == 429:  # Handle Too Many Requests (Rate limiting)
        retry_after = response.headers.get('Retry-After')
        print(f"Rate limit exceeded. Retry-After: {retry_after} seconds")
        return False, int(retry_after) if retry_after else None  # Return Retry-After header value
    else:
        print(f"Error: {response.status_code} - {response.text}")
        return False, None  # Failed call

# Function to display the countdown for retry-after period
def display_retry_after_countdown(retry_after_seconds):
    while retry_after_seconds > 0:
        print(f"Waiting for {retry_after_seconds} seconds before resuming...", end="\r")
        time.sleep(1)
        retry_after_seconds -= 1
    print()  # Move to the next line after countdown finishes

# Main function to handle configurable parameters
def main(api_url, frequency, content_type):
    headers = {'Content-Type': content_type}
    retry_after_time = None
    initial_requests = 20

    for i in range(initial_requests):  # Initially call the API 20 times
        success, retry_after_time = call_api(api_url, headers)
        if not success and retry_after_time:  # If 429 is encountered, break the loop
            break
        time.sleep(frequency)  # Wait for the specified frequency

    # Handle Retry-After if we hit the rate limit
    if retry_after_time:
        display_retry_after_countdown(retry_after_time)  # Display countdown before resuming

    # Resume sending messages after waiting
    try:
        while True:
            success, retry_after_time = call_api(api_url, headers)
            if not success and retry_after_time:
                print(f"Rate limit hit again. Waiting for {retry_after_time} seconds...")
                display_retry_after_countdown(retry_after_time)  # Display countdown if rate limit is hit again
            else:
                time.sleep(frequency)  # Otherwise, continue at regular intervals
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
