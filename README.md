# FTPFetcher
Download files from a FTP only once.

## Parameters
* `-t`, `--threads`: The number of threads (downloaders) to use (_default `1`_).

* `-p`, `--properties`: The settings file to use (see [Configuration file](#configuration-file)).

* `-db`, `--database`: The database file (SQLite) to store the history of downloaded files (the history is reset after 15 days) (_default: `./FTPFetcher.db`_).

## Configuration file
This is a JSON file following this pattern:
```json5
{
	"ftpHost": "url.to.ftp",
	"ftpUser": "user",
	"ftpPass": "password",
	"folders": [
		{
			"localFolder": "./my/local/folder",
			"ftpFolder": "/distant/folder",
			"recursive": true, // default: false
			"fileFilter": ".*\\.(jpg|mp4|mov)$", // default: ".*"
            "isFilenameDate": false, // default: true
		}
	]
}
```
