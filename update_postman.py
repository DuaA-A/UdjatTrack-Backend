import json
import os

def create_request(name, method, path, query_params=None):
    if query_params is None:
        query_params = []
    
    req = {
        "name": name,
        "request": {
            "method": method,
            "header": [
                {
                    "key": "Authorization",
                    "value": "Bearer {{accessToken}}",
                    "type": "text"
                }
            ],
            "url": {
                "raw": "{{baseUrl}}/" + path + ("?" + "&".join([f"{q['key']}={q['value']}" for q in query_params]) if query_params else ""),
                "host": ["{{baseUrl}}"],
                "path": path.split("/"),
                "query": query_params
            }
        },
        "response": []
    }
    return req

def main():
    file_path = r'c:\Users\dodoa\OneDrive\Desktop\UdjatTrack-Backend\Udjat Track.postman_collection.json'
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # Missing endpoints to add
    report_requests = [
        create_request("Export Report PDF", "GET", "reports/export/pdf"),
        create_request("Export Report CSV", "GET", "reports/export/csv"),
        create_request("Report Summary Table", "GET", "reports/table"),
        create_request("Filter Report by Driver and Vehicle", "GET", "reports/summary", [
            {"key": "driverId", "value": "{{driverId}}"},
            {"key": "vehicleId", "value": "{{vehicleId}}"}
        ])
    ]
    
    vehicle_requests = [
        create_request("Filter Vehicle by Plate Number", "GET", "vehicles", [
            {"key": "plateNumber", "value": "{{plateNumber}}"}
        ]),
        create_request("Get Vehicle with Driver Details", "GET", "vehicles/{{vehicleId}}/with-driver")
    ]
    
    dashboard_requests = [
        create_request("Active Trips", "GET", "dashboard/active-trips"),
        create_request("Filter Active Trips by Trip ID", "GET", "dashboard/active-trips", [
            {"key": "tripId", "value": "{{tripId}}"}
        ]),
        create_request("Trip Summary Table", "GET", "dashboard/trip-summary-table")
    ]

    for item in data.get('item', []):
        if item.get('name') == 'Report Management':
            # Avoid duplicates
            existing = [r.get('name') for r in item.get('item', [])]
            for r in report_requests:
                if r['name'] not in existing:
                    item['item'].append(r)
        elif item.get('name') == 'Trip Management':
            for sub in item.get('item', []):
                if sub.get('name') == 'Vehicles':
                    existing = [r.get('name') for r in sub.get('item', [])]
                    for r in vehicle_requests:
                        if r['name'] not in existing:
                            sub['item'].append(r)
        elif item.get('name') == 'Fleet & Dashboard':
            existing = [r.get('name') for r in item.get('item', [])]
            for r in dashboard_requests:
                if r['name'] not in existing:
                    item['item'].append(r)
            
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2)

if __name__ == '__main__':
    main()
