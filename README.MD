# imapchecker


## Main config file
The main config file (`imapchecker.properties`) simply lists a name and a filename as key-value pairs.

```
server1=server.properties
server2=anotherconfig.properties
```

## Per account config file
The following settings are used in a per-account configuration.

```
username=username
password=password
hostname=imap.server
ntfytoken=token
ntfyurl=https://ntfy.sh/topic
filterfile=filters.txt
junkpath=inbox.Junk
```