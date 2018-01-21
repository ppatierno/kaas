package io.strimzi.controller.cluster.operations.openshift;

import io.fabric8.openshift.api.model.BuildConfig;
import io.strimzi.controller.cluster.OpenShiftUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteBuildConfigOperation extends OpenShiftOperation {
    private static final Logger log = LoggerFactory.getLogger(DeleteBuildConfigOperation.class.getName());
    private final String namespace;
    private final String name;

    public DeleteBuildConfigOperation(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    @Override
    public void execute(Vertx vertx, OpenShiftUtils os, Handler<AsyncResult<Void>> handler) {
        vertx.createSharedWorkerExecutor("kubernetes-ops-pool").executeBlocking(
                future -> {
                    if (os.exists(namespace, name, BuildConfig.class)) {
                        try {
                            log.info("Deleting BuildConfig {}", name);
                            os.delete(namespace, name, BuildConfig.class);
                            future.complete();
                        } catch (Exception e) {
                            log.error("Caught exception while deleting BuildConfig", e);
                            future.fail(e);
                        }
                    }
                    else {
                        log.warn("BuildConfig {} doesn't exists", name);
                        future.complete();
                    }
                },
                false,
                res -> {
                    if (res.succeeded()) {
                        log.info("BuildConfig {} has been deleted", name);
                        handler.handle(Future.succeededFuture());
                    }
                    else {
                        log.error("BuildConfig deletion failed: {}", res.result());
                        handler.handle(Future.failedFuture((Exception)res.result()));
                    }
                }
        );
    }
}
