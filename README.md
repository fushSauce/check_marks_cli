# CheckMarks (WIP)
- Program to check whether marks page has changed and notifying me if so, so that I'm not constantly refreshing the page.
- How it works: 
  - we run an instance of seleniums standalone [firefox](https://hub.docker.com/r/selenium/standalone-firefox) (firefox container) and connect to it using the RemoteWebDriver in the container running our code (checkmarks container), which uses the browser to login to the page and see if the resulting html is different, if so sending an email through gmail to recipient set in the secret.json file.
  - This program is called routinely by [chadburn](https://github.com/PremoWeb/chadburn) (chadburn container) which acts like a cronjob periodically executing a script that runs the checkmarks cli program, using chadburn so host doesn't need to manually setup a cronjob to exec script on checkmarks container.
## Install / Setup / Run
- requirements: docker
- clone repository `https://github.com/fushSauce/check_marks_cli`
- set secret.json file object properties to match your usernames and passwords, and make sure it stays in project root dir.
```json
[
  {
    "type": "ecs",
    "username": "<yourecsusername>",
    "password": "<yourecspassword>"
  },
  {
    "type": "sender",
    "username": "<yoursendergmailaddress>",
    "password": "<yoursendergmailapppassword>"
  },
  {
    "type": "receiver",
    "username": "<yourreceivergmailaddress>"
  }
]
```
- run `gradle shadowJar && docker build -t checkmarks .`
- run `docker compose up -d`
- can also see the standalone browsers actions being taken through `localhost:7900 pass: 'secret'`