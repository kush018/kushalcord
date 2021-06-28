# kushalcord

The kushalcord discord bot - a discord bot written in java with the help of discord4j

## Initial Setup

In order for the bot to work, we need to define some properties or attributes for the bot in a file called "config"

To understand the syntax of the config file, see the demo config file: [democonfig](democonfig)

Now, set the following attributes in the config file

api_key -> THE API KEY OF YOUR BOT

gh_repo -> LINK TO GITHUB REPOSITORY OF BOT'S SOURCE CODE

bot_inv -> INVITE LINK OF BOT

db_name -> PATH TO DATABASE FILE (optional - if not specified default database file path will be used, which is: database.db)

## Deploying the discord bot

```
# for windows computers
.\mvnw package
# for linux computers
chmod +x mvnw
./mvnw package

# this creates a jar file (with all the dependencies) in the target/ directory

# to run the jar file
java -jar target/kushalcord-1.0-SNAPSHOT-jar-with-dependencies.jar

# NOTE: Do not change directory to target/ and directly run the jar because the conf/ folder with all the configurations is not there in the target/ directory by default. Basically all you need to make sure is that the conf folder is in the current directory while running the .jar file.
```

### Deploying using docker

```
# create an empty database file
# (you can replace database.db with desired database file)
touch database.db

# create config file

# run the docker image by
docker run -d -v $(pwd)/config:/usr/kushalcord/config -v $(pwd)/database.db:/usr/kushalcord/database.db kush018/kushalcord:latest
# you can replace database.db with the database file you will use
# the -d option runs the container as a background process - you can remove this if you want to 
# docker image is pulled automatically from docker hub
```

## kushalcord user manual

By default, kushalcord uses the command prefix "kc ". This means that if I wanted to give a command "rank" I would type "kc rank". However, this can be changed easily by editing the code.

Valid commands of this bot are:

```
help - prints general help menu
help <command> - prints help menu for command

ping - causes bot to reply with Pong!
(used to test if bot is running properly or not)

ask <question> - ask a yes/no type question to the bot to hear the answer
(its not real its just for fun. the bot only gives random answers)

bal - tells your bank balance
bal <mention1> <mention2> ... - tells the bank balance of all the users mentioned

daily - collect your daily allowance

transfer <amt> <mention> - transfers amt coins to user mentioned

rank - tells ur xp, level and rank
rank <mention1> <mention2> ... - tells the rank of all users mentioned

toprankers <n> - prints the leaderboard of the top n ranks in the guild xp system
toprankers - same as the above commands where n = 10

work <job> - work for a job
currently available jobs: hacker, paanwala

say <something> - make the bot say something and delete the command so that it looks like the bot said it out of its own free will

bj <amount> - gamble your money away in a game of blackjack

invite - get the link to invite this bot to other servers :)

github - get the link the the github repository containing the code for this bot
```

## Updating the bot

To update the bot's source code, simply type:

```
git pull https://github.com/kush018/kushalcord.git master
```

The source code is now updated, so now build the project again and you can run the updated project.
