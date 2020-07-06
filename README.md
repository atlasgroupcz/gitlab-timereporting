This is a GitLab time reporting tool.  
Works for local gitlab installations.  
Uses direct Gitlab database exports to CSV to create data for various time reports. 

## Preparing data

#### Exporting data from GitLab

Export GitLab stuff first, call `sudo export.sh` on your GitLab server

```$bash
[trehak@gitlab-server ~]$ sudo ./export.sh 
[sudo] password for trehak: 
Exporting users to /tmp/gitlab-export-TMAwib7wqh/users.csv
COPY 95
Exporting issues to /tmp/gitlab-export-TMAwib7wqh/issues.csv
COPY 5687
Exporting labels to /tmp/gitlab-export-TMAwib7wqh/labels.csv
COPY 554
Exporting timelogs to /tmp/gitlab-export-TMAwib7wqh/timelogs.csv
COPY 202
Exporting label_links to /tmp/gitlab-export-TMAwib7wqh/label_links.csv
COPY 7894
Exporting boards to /tmp/gitlab-export-TMAwib7wqh/boards.csv
COPY 51
Exporting projects to /tmp/gitlab-export-TMAwib7wqh/projects.csv
COPY 285
Exporting merge_requests to /tmp/gitlab-export-TMAwib7wqh/merge_requests.csv
COPY 10221
Exporting namespaces to /tmp/gitlab-export-TMAwib7wqh/namespaces.csv
COPY 137
  adding: namespaces.csv (deflated 72%)
  adding: timelogs.csv (deflated 79%)
  adding: label_links.csv (deflated 77%)
  adding: projects.csv (deflated 67%)
  adding: boards.csv (deflated 74%)
  adding: merge_requests.csv (deflated 76%)
  adding: labels.csv (deflated 77%)
  adding: issues.csv (deflated 80%)
  adding: users.csv (deflated 59%)
[trehak@gitlab-server ~]$ ls export.zip
export.zip
[trehak@gitlab-server ~]$ 

```
#### Importing data to reporting tool

Upload GitLab exports to reporting backend  
`curl -F 'file=@export.zip' http://localhost:8080/rest/timelogs/upload`

#### Backend utility endpoints
  
`/rest/timelogs/hasData` - any data available?
`/rest/timelogs/getDataTimestamp` - last data upload timestamp
`/rest/timelogs/getDataTimestamp` - last data upload timestamp
`/rest/timelogs/users` - list available user IDs

## Report types

#### Hierarchy reports

Hierarchical reports can be used to produce d3js visualizations:  
https://observablehq.com/@d3/nested-treemap
https://observablehq.com/@d3/cascaded-treemap
https://observablehq.com/@d3/treemap
https://observablehq.com/@d3/zoomable-treemap
https://observablehq.com/@d3/zoomable-sunburst 

All possible hierarchy components are - `NAMESPACE`, `PROJECT`, `ISSUE`, `USER`, `PRODUCT`  
To collect `PRODUCT` info, put `product-XXXX` in your label descriptions in GitLab, where XXXX is a product name


`/rest/timelogs/hierarchyComponents?from=2020-07-01&to=2020-08-01` - returns available hierarchy components for selected time periods 

```$json
{
  "NAMESPACE" : [ "Project X", "License server", "Document management", "java-projects" ],
  "PROJECT" : [ "Academia", "AladinEU", "GraphQL", "MSAD" ],
  "ISSUE" : [ "[1] Bugfix 1", "[2] New feature", "[3] Cool new feature" ],
  "USER" : [ "George Rehak", "Manuel Bacigalupo", "Juanita Esmeralda Linda" ],
  "PRODUCT" : [ "unknown", "main-stuff", "side-kick" ]
}
```

`/rest/timelogs/hierarchy?from=2020-07-01&to=2020-08-01&elements=NAMESPACE&elements=PROJECT&elements=ISSUE&elements=USER` - produces a hierarchical breakdown of work reports by namespace -> project -> issue -> user

`/rest/timelogs/hierarchy?from=2020-07-01&to=2020-08-01&elements=PRODUCT&elements=ISSUE&elements=USER` - produces a hierarchical breakdown of work reports by product -> issue -> user

etc. 



#### Calendar reports

Produces calendar data view for d3js calendar visualization.   
Each day represents contains work details for user (basically visualized timesheet).     

https://observablehq.com/@d3/calendar-view

`/rest/timelogs/userCalendar/{year}/{userId}`

for example `/rest/timelogs/userCalendar/2020/31` produces year calendar view for given user, each


#### Excel reports

`/rest/timelogs/timesheet?from=2020-07-01&to=2020-08-01` - produces Excel workbook for give period, one user per sheet