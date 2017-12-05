# Fn Project JRestless Blogging Example

Author: [Rae Jeffries-Harris](https://github.com/RaeJ) 

This is an example JAX-RS app that can be deployed on the Fn platform.

## What?

[JRestless](http://github.com/bbilger/jrestless) allows you to create FaaS applications using [JAX-RS](https://github.com/jax-rs).
We are adding [support for using JRestless on Fn](https://github.com/bbilger/jrestless/pull/43).

This means you can use all the JAX-RS features you're used to, `@Path`, `@GET`, `@QueryParam` -
all the marshalling and content-types, all the routing. All of it, in a FaaS function. JRestless uses
[Jersey](https://jersey.github.io/) internally so you have the full capability of the reference JAX-RS
implementation.

If you take advantage of [FnProject's Hot Functions](https://github.com/fnproject/fn/blob/master/docs/hot-functions.md)
you can build a responsive application which only runs when it's needed.

This project is an example of how to use JRestless build a JAX-RS app on FnProject.

## How does it work?

The code in `com.example.fnjrestless.blog` is a JAX-RS app:

  * `BloggingResource.java` - defines the routes
  * `BloggPost.java` - POJO domain object
  * `BlogStore.java` - Database connection code
  * `BloggingApp.java` - 5 lines of code to point JRestless at our app
  
Check out the code, then in the root of the project run:

```shell
$ fn build

...snip...

Function raej/jrest:0.0.4 built successfully.
```

Make sure you have an fn server running (See [the tutorial](https://github.com/fnproject/tutorials/tree/master/Introduction#starting-fn-server) - TL;DR `fn start`)

Then create your app:

```shell
$ fn apps create jaxrs
Successfully created app:  jaxrs

$ fn deploy --app jaxrs --local
```

You will need a database, easy way is to run the `./start-mysql.sh` included in the repo.

Some config for your app:

```shell
## The IP address of the host from inside a container
$ export DOCKER_HOST_IP=$(docker inspect --type container -f '{{.NetworkSettings.Gateway}}' fnserver)

fn apps config set jaxrs DB_URL "jdbc:mysql://${DOCKER_HOST_IP}/POSTS"
fn apps config set jaxrs DB_DRIVER com.mysql.jdbc.Driver
fn apps config set jaxrs DB_USER jaxrs
fn apps config set jaxrs DB_PASSWORD SgRoV3s
```

Map the routes used by the app to the function:
(use whichever image version was published in the previous deploy )

```shell
fn routes create jaxrs /route/html --image raej/jrest:0.0.2
fn routes create jaxrs /route/blogs --image raej/jrest:0.0.2
# fn routes create jaxrs /route/add  <-- this was added by `fn deploy` earlier
```

All 3 of these routes will point to the same container image.

## Bask in the glory of your creation

Browse to [http://localhost:8080/r/jaxrs/route/html](http://localhost:8080/r/jaxrs/route/html) to see the blog's UI.

There is a simple HTML page served from `/html`, which fetches data from `/blogs` and posts new blog entries to `/add`.

## FAQ / Notes

### QUESTION: How many Function containers will be used?

Fn treats each route independently, so there will be *at least* one per endpoint that you use. There may be more than one, as container instances will be created dynamically to cope with high load.


### QUESTION: What about binding variables from path params?

Fn currently does not support wildcards in routes. Issues [#170](https://github.com/fnproject/fn/issues/170) and [#256](https://github.com/fnproject/fn/issues/256) track current proposals to fix this.


