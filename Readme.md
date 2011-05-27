# Pulse

Real-time Heroku operations dashboard.

## Running `engine` producer process in EC2:

    $ bin/shell
    $ clj -m pulse.engine

## Subscribing to raw stats feed:

    $ export REDIS_URL=<redis-url>
    $ redis-cliu subscribe stats

## Running `term` consumer process locally:

    $ export REDIS_URL=<redis-url>
    $ clj -m pulse.term

## Running web app locally:

    $ lein deps
    $ export REDIS_URL=<redis-url>
    $ clj -m pulse.web

## Running web app on Heroku:

    $ heroku create opspulse --stack cedar
    $ heroku addons:add ssl:piggyback
    $ heroku addons:add redistogo:small
    $ heroku routes:create
    $ heroku config:add FORWARDER_HOSTS=$FORWARDER_HOSTS LOGPLEX_HOST=$LOGPLEX_HOSTS
    $ heroku config:add WEBSOCKET_URL=ws://<route-ip>:<route-port>/stats
    $ heroku config:add WEB_AUTH=<user>:<pass>
    $ git push heroku master
    $ heroku scale web 1
    $ heroku scale sock 1
    $ heroku routes:attach <route-url> sock.1
    $ open https://opspulse.heroku.com