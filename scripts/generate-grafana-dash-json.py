import json

def generate_grafana_dashboard():
    dashboard = {
        "dashboard": {
            "id": None,
            "uid": None,
            "title": "API Monitoring Dashboard",
            "tags": ["API", "Monitoring"],
            "timezone": "browser",
            "schemaVersion": 16,
            "version": 1,
            "refresh": "5s",
            "panels": []
        },
        "overwrite": True
    }

    # Template for panels
    def create_panel(title, expr, panel_id, y_axis_label="ms"):
        return {
            "type": "graph",
            "title": title,
            "id": panel_id,
            "datasource": "Prometheus",  # Change this to your datasource name
            "targets": [
                {
                    "expr": expr,
                    "intervalFactor": 2,
                    "refId": "A"
                }
            ],
            "xaxis": {
                "mode": "time"
            },
            "yaxes": [
                {
                    "format": y_axis_label,
                    "label": "Time",
                    "logBase": 1,
                    "show": True
                }
            ],
            "lines": True,
            "fill": 1,
            "linewidth": 1,
            "legend": {
                "show": True
            },
            "gridPos": {
                "h": 9,
                "w": 12,
                "x": 0,
                "y": 0
            }
        }

    panels = [
        {"title": "Latency (99th Percentile)", "expr": 'histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))'},
        {"title": "Request Rate (per min)", "expr": 'rate(http_server_requests_total[1m])'},
        {"title": "Request Duration (95th Percentile)", "expr": 'histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))'},
        {"title": "Request Errors (4xx)", "expr": 'sum(rate(http_server_requests_total{status=~"4.."}[5m])) by (uri)'},
        {"title": "Request Errors (5xx)", "expr": 'sum(rate(http_server_requests_total{status=~"5.."}[5m])) by (uri)'},
        {"title": "Request Duration (Median)", "expr": 'histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))'},
        {"title": "Active Requests", "expr": 'sum(http_server_requests_active_seconds_gcount) by (uri)'},
        {"title": "Max Request Time", "expr": 'max(http_server_requests_seconds_max) by (uri)'},
        {"title": "Min Request Time", "expr": 'min(http_server_requests_seconds_max) by (uri)'},
        {"title": "Request Success Rate", "expr": 'sum(rate(http_server_requests_total{status=~"2.."}[5m])) by (uri)'},
        {"title": "Request Count by Status Code", "expr": 'sum(rate(http_server_requests_total[5m])) by (status)'},
        {"title": "Total Request Count", "expr": 'sum(rate(http_server_requests_total[5m])) by (uri)'},
        {"title": "JVM Memory Usage", "expr": 'jvm_memory_used_bytes'},
        {"title": "JVM CPU Usage", "expr": 'process_cpu_usage'},
        {"title": "System Load Average", "expr": 'system_load_average_1m'},
        {"title": "Active Threads", "expr": 'jvm_threads_live_threads'},
        {"title": "Idle Connections", "expr": 'jdbc_connections_idle'},
        {"title": "Active Connections", "expr": 'jdbc_connections_active'},
        {"title": "Database Query Duration", "expr": 'sum(rate(spring_data_repository_invocations_seconds_sum[1m])) by (repository)'},
        {"title": "Cache Hits and Misses", "expr": 'sum(rate(cache_gets_total[5m])) by (result)'}
    ]

    panel_id = 1
    for panel in panels:
        dashboard["dashboard"]["panels"].append(create_panel(panel["title"], panel["expr"], panel_id))
        panel_id += 1

    return json.dumps(dashboard, indent=2)

# Save the generated dashboard JSON to a file
dashboard_json = generate_grafana_dashboard()

with open('grafana_dashboard.json', 'w') as f:
    f.write(dashboard_json)

print("Dashboard JSON generated successfully and saved to 'grafana_dashboard.json'")
