# kushalcord

The kushalcord discord bot - a discord bot written in java with the help of discord4j

## Initial Setup

In order for the bot to work, create a "bot account" in discord.
If you don't have that set up, set one up.

Once, thats done, take note of the API key. Then, 
```
mkdir conf
cd conf
```
This will change your directory to the newly created conf directory.
Then,
```
echo "ENTER API KEY HERE" > api_key
```
This will store your API key in a file.

For kushalcord to work properly, it needs access to a postgresql database. So, set one up and take note of:
1) The ip address of the database
2) The name of the database
3) The credentials (username and password) required to access the database

Then,
```
echo "database_ip/database_name" > db_addr
echo "username" > db_creds
echo "password" >> db_creds
```
This will store your database details.

If this setup is not done properly, the bot will not work.

## Deploying the discord bot

```
# for windows computers
mvnw package
# for linux computers
chmod +x mvnw
./mvnw package

# this creates a jar file (with all the dependencies) in the target/ directory

# to run the jar file
java -jar target/kushalcord-1.0-SNAPSHOT.jar
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

spam <message> - spams a message 5 times
spam <n> <message> - spams a message n times
(use at your own risk lol)

delete <n> - deletes the last n messages, and the delete message request

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
```