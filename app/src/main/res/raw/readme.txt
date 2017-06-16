The res/raw/ directory is a place where you - the researcher - can customize the app to your deployment.

There are things you need to do so that the app can communicate with your own server:
1) Add a file named server_hostname.txt and within it a single line that specifies the domain name of your server
(e.g. behaviorlab.myuniversity.edu), or its IP address.
2) If you wish your data to be communicated over secured connection (https protocol, including encryption and server-authentication),
you need to add a file named server_certificate.crt containing the public certificate of your server that matches the private key that your server will use for SSL.

This directory also has textual list files that specify lists of labels to be used for self-reporting.
These are the labels that will appear in the label-selection menus.
You are free to change these lists to offer the labels of interest for your research. The labels should have no commas.
The labels in secondary_activities_list.txt are presented in the "secondary activities" view in the app:
that view allows selecting multiple labels, and there's a side-bar index with topic-link for quick finding of the relevant labels.
In the text file you can specify for each label the topics it should appear under:
follow the label with a pipe (|) and then the names of the topics, separated with commas.