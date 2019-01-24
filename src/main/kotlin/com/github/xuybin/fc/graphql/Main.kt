package com.github.xuybin.fc.graphql

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import graphql.GraphQL
import graphql.execution.preparsed.PreparsedDocumentEntry
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.public
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.ApacheServer
import org.http4k.server.asServer
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

fun main(args: Array<String>) {
    val appContext = ServiceLoader.load(GApp::class.java).first()
    // 初始化
    val logger = LoggerFactory.getLogger(appContext::class.java)
    appContext.init(args)
    logger.info("${appContext.javaClass.canonicalName} init ${args.joinToString()}")

    // 获取启动配置
    var serverPort = -1
    var origins = mutableSetOf<String>()
    var headers = mutableSetOf<String>()
    var methods = mutableSetOf<Method>()
    var corsUrl=""
    val fiter: (Pair<String, String>) -> Unit = {
        if (it.first.equals("fcg.cors-url") && corsUrl.isBlank()) corsUrl = it.second.trim()
        if (it.first.equals("fcg.port") && (serverPort < 0 || serverPort > 65536)) serverPort = it.second.toInt()
        if (it.first.equals("fcg.cors-policy.origins") && origins.isEmpty()) origins.addAll(it.second.split(",").mapNotNull { v->if (v.isNotBlank()) v else null })
        if (it.first.equals("fcg.cors-policy.headers") && headers.isEmpty()) headers.addAll(it.second.split(",").mapNotNull { v->if (v.isNotBlank()) v else null })
        if (it.first.equals("fcg.cors-policy.methods") && methods.isEmpty()) methods.addAll(it.second.split(",").mapNotNull { v ->
            when {
                v.equals(Method.GET.name, true) -> Method.GET
                v.equals(Method.POST.name, true) -> Method.POST
                v.equals(Method.PUT.name, true) -> Method.PUT
                v.equals(Method.DELETE.name, true) -> Method.DELETE
                v.equals(Method.OPTIONS.name, true) -> Method.OPTIONS
                v.equals(Method.TRACE.name, true) -> Method.TRACE
                v.equals(Method.PATCH.name, true) -> Method.PATCH
                v.equals(Method.PURGE.name, true) -> Method.PURGE
                v.equals(Method.HEAD.name, true) -> Method.HEAD
                else -> null
            }
        })
    }
    // 按application.properties->bootstrap.properties->defaults.properties顺序，以优先取到的配置为准
    try {
        loadProperties(appContext::class.java, "/application.properties").forEach {
            fiter(it)
        }
    } catch (ex: Throwable) {
    }
    try {
        loadProperties(appContext::class.java, "/bootstrap.properties").forEach {
            fiter(it)
        }
    } catch (ex: Throwable) {
    }
    try {
        loadProperties(GRequest::class.java, "/com/github/xuybin/fc/graphql/defaults.properties").forEach {
            fiter(it)
        }
    } catch (ex: Throwable) {
    }

    // 缓存1000次不同的query
    val cache: Cache<String, PreparsedDocumentEntry> = Caffeine.newBuilder().maximumSize(1000).build()
    val graphQL = GraphQL.newGraphQL(appContext.initGraphqlSchema())
        .preparsedDocumentProvider(cache::get)
        .build()
    routes(
        "/graphiql.html" bind Method.GET to {
            // corsUrl 用于跨域测试，典型的设置成http://localhost/graphql 但网页上访问http://127.0.0.1/graphiql.html
            Response(Status.OK).public().body(if (corsUrl.isNotBlank()) graphiqlHtml.replace("'/graphql'",corsUrl) else graphiqlHtml)
        },
        "/graphql" bind Method.POST to {
            try {
                val startInstant = Instant.now()
                Response(Status.OK).body(
                    appContext.getContext().getExecutionInput(it.bodyString()).execute(graphQL, startInstant)
                )
            } catch (ex: Throwable) {
                Response(Status.OK).body(
                    mapOf(
                        "errors" to listOf(
                            GErrExecute(
                                GErrType.UnknownSerializable,
                                "at main ${ex.message}"
                            ).toMap()
                        )
                    ).let {
                        appContext.toJson(it)
                    }
                )
            }
        }).apply {
        if (origins.isNotEmpty() || headers.isNotEmpty() || methods.isNotEmpty())
            withFilter(
                ServerFilters.Cors(
                    CorsPolicy(
                        origins.toList(),
                        headers.toList(),
                        methods.toList()
                    )
                )
            )
    }
        .asServer(ApacheServer(serverPort))
        .start()
    logger.info("start server at ${serverPort}")
}

val graphiqlHtml = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body {
            height: 100%;
            margin: 0;
            width: 100%;
            overflow: hidden;
        }
        #graphiql {
            height: 100vh;
        }
    </style>

    <script src="//cdn.bootcss.com/es6-promise/4.1.1/es6-promise.auto.min.js"></script>
    <script src="//cdn.bootcss.com/fetch/0.9.0/fetch.min.js"></script>
    <script src="//cdn.bootcss.com/react/15.6.2/react.min.js"></script>
    <script src="//cdn.bootcss.com/react-dom/15.6.2/react-dom.min.js"></script>

    <link rel="stylesheet" href="//cdn.bootcss.com/graphiql/0.12.0/graphiql.min.css" />
    <script src="//cdn.bootcss.com/graphiql/0.12.0/graphiql.min.js"></script>

</head>
<body>
<div id="graphiql">Loading...</div>
<script>
    // Parse the search string to get url parameters.
    var search = window.location.search;
    var parameters = {};
    search.substr(1).split('&').forEach(function (entry) {
        var eq = entry.indexOf('=');
        if (eq >= 0) {
            parameters[decodeURIComponent(entry.slice(0, eq))] =
                decodeURIComponent(entry.slice(eq + 1));
        }
    });

    // if variables was provided, try to format it.
    if (parameters.variables) {
        try {
            parameters.variables =
                JSON.stringify(JSON.parse(parameters.variables), null, 2);
        } catch (e) {
            // Do nothing, we want to display the invalid JSON as a string, rather
            // than present an error.
        }
    }

    // When the query and variables string is edited, update the URL bar so
    // that it can be easily shared
    function onEditQuery(newQuery) {
        parameters.query = newQuery;
        updateURL();
    }

    function onEditVariables(newVariables) {
        parameters.variables = newVariables;
        updateURL();
    }

    function onEditOperationName(newOperationName) {
        parameters.operationName = newOperationName;
        updateURL();
    }

    function updateURL() {
        var newSearch = '?' + Object.keys(parameters).filter(function (key) {
            return Boolean(parameters[key]);
        }).map(function (key) {
            return encodeURIComponent(key) + '=' +
                encodeURIComponent(parameters[key]);
        }).join('&');
        history.replaceState(null, null, newSearch);
    }

    // Defines a GraphQL fetcher using the fetch API. You're not required to
    // use fetch, and could instead implement graphQLFetcher however you like,
    // as long as it returns a Promise or Observable.
    function graphQLFetcher(graphQLParams) {
        // This example expects a GraphQL server at the path /graphql.
        // Change this to point wherever you host your GraphQL server.
        return fetch('/graphql', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(graphQLParams),
            credentials: 'include',
        }).then(function (response) {
            return response.text();
        }).then(function (responseBody) {
            try {
                return JSON.parse(responseBody);
            } catch (error) {
                return responseBody;
            }
        });
    }

    // Render <GraphiQL /> into the body.
    // See the README in the top level of this module to learn more about
    // how you can customize GraphiQL by providing different values or
    // additional child elements.
    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: graphQLFetcher,
            query: parameters.query,
            variables: parameters.variables,
            operationName: parameters.operationName,
            onEditQuery: onEditQuery,
            onEditVariables: onEditVariables,
            onEditOperationName: onEditOperationName
        }),
        document.getElementById('graphiql')
    );
</script>
</body>
</html>
"""