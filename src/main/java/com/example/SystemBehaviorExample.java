package com.example;

import java.time.Duration;

import com.aerospike.client.policy.ReadModeSC;
import com.aerospike.client.policy.Replica;
import com.aerospike.policy.Behavior;
import com.aerospike.policy.Settings;

/**
 * Demonstrates usage of the system() selector category for configuring
 * system-level settings including transaction verification, rollback, 
 * connections, circuit breaker, and cluster refresh.
 */
public class SystemBehaviorExample {

    public static void main(String[] args) {
        // Example 1: Configure transaction verification settings
        Behavior customTxnVerify = Behavior.DEFAULT.deriveWithChanges("customTxnVerify", builder -> builder
            .on(Behavior.Selectors.system().txnVerify(), ops -> ops
                .consistency(ReadModeSC.LINEARIZE)
                .replicaOrder(Replica.MASTER)
                .maximumNumberOfCallAttempts(10)
                .waitForCallToComplete(Duration.ofSeconds(5))
                .abandonCallAfter(Duration.ofSeconds(15))
                .delayBetweenRetries(Duration.ofSeconds(2))
            )
        );
        
        System.out.println("Example 1: Transaction Verification Behavior");
        System.out.println(customTxnVerify);
        System.out.println();

        // Example 2: Configure transaction rollback settings
        Behavior customTxnRoll = Behavior.DEFAULT.deriveWithChanges("customTxnRoll", builder -> builder
            .on(Behavior.Selectors.system().txnRoll(), ops -> ops
                .replicaOrder(Replica.MASTER)
                .maximumNumberOfCallAttempts(8)
                .waitForCallToComplete(Duration.ofSeconds(4))
                .abandonCallAfter(Duration.ofSeconds(12))
                .delayBetweenRetries(Duration.ofSeconds(1))
            )
        );
        
        System.out.println("Example 2: Transaction Rollback Behavior");
        System.out.println(customTxnRoll);
        System.out.println();

        // Example 3: Configure connection pool settings
        Behavior customConnections = Behavior.DEFAULT.deriveWithChanges("customConnections", builder -> builder
            .on(Behavior.Selectors.system().connections(), ops -> ops
                .minimumConnectionsPerNode(10)
                .maximumConnectionsPerNode(200)
                .maximumSocketIdleTime(Duration.ofSeconds(120))
            )
        );
        
        System.out.println("Example 3: Connection Pool Behavior");
        System.out.println(customConnections);
        System.out.println();

        // Example 4: Configure circuit breaker settings
        Behavior customCircuitBreaker = Behavior.DEFAULT.deriveWithChanges("customCircuitBreaker", builder -> builder
            .on(Behavior.Selectors.system().circuitBreaker(), ops -> ops
                .numTendIntervalsInErrorWindow(2)
                .maximumErrorsInErrorWindow(50)
            )
        );
        
        System.out.println("Example 4: Circuit Breaker Behavior");
        System.out.println(customCircuitBreaker);
        System.out.println();

        // Example 5: Configure cluster refresh settings
        Behavior customRefresh = Behavior.DEFAULT.deriveWithChanges("customRefresh", builder -> builder
            .on(Behavior.Selectors.system().refresh(), ops -> ops
                .tendInterval(Duration.ofSeconds(2))
            )
        );
        
        System.out.println("Example 5: Cluster Refresh Behavior");
        System.out.println(customRefresh);
        System.out.println();

        // Example 6: Configure multiple system categories at once
        Behavior comprehensiveSystemBehavior = Behavior.DEFAULT.deriveWithChanges("comprehensive", builder -> builder
            .on(Behavior.Selectors.system().txnVerify(), ops -> ops
                .consistency(ReadModeSC.LINEARIZE)
                .maximumNumberOfCallAttempts(10)
            )
            .on(Behavior.Selectors.system().connections(), ops -> ops
                .minimumConnectionsPerNode(5)
                .maximumConnectionsPerNode(150)
            )
            .on(Behavior.Selectors.system().circuitBreaker(), ops -> ops
                .maximumErrorsInErrorWindow(75)
            )
            .on(Behavior.Selectors.system().refresh(), ops -> ops
                .tendInterval(Duration.ofMillis(500))
            )
        );
        
        System.out.println("Example 6: Comprehensive System Behavior");
        System.out.println(comprehensiveSystemBehavior);
        System.out.println();

        // Example 7: Demonstrate cascading behaviors
        Behavior parentBehavior = Behavior.DEFAULT.deriveWithChanges("parent", builder -> builder
            .on(Behavior.Selectors.system().connections(), ops -> ops
                .maximumConnectionsPerNode(100)
            )
        );
        
        Behavior childBehavior = parentBehavior.deriveWithChanges("child", builder -> builder
            .on(Behavior.Selectors.system().connections(), ops -> ops
                .maximumConnectionsPerNode(250) // Override parent
            )
            .on(Behavior.Selectors.system().refresh(), ops -> ops
                .tendInterval(Duration.ofSeconds(3)) // Add new setting
            )
        );
        
        System.out.println("Example 7: Cascading Behaviors");
        System.out.println("Parent: " + parentBehavior);
        System.out.println("Child: " + childBehavior);
        System.out.println();

        // Example 8: Retrieve system settings
        System.out.println("Example 8: Retrieving System Settings");
        
        Behavior testBehavior = Behavior.DEFAULT.deriveWithChanges("retrievalExample", builder -> builder
            .on(Behavior.Selectors.system().connections(), ops -> ops
                .maximumConnectionsPerNode(200)
                .minimumConnectionsPerNode(5)
            )
            .on(Behavior.Selectors.system().circuitBreaker(), ops -> ops
                .maximumErrorsInErrorWindow(50)
            )
        );
        
        // Retrieve and display system settings
        Settings connectionsSettings = testBehavior.getSettings(
            Behavior.OpKind.SYSTEM_CONNECTIONS, 
            Behavior.OpShape.SYSTEM, 
            Behavior.Mode.ANY
        );
        System.out.println("Connections Settings:");
        System.out.println("  Min connections per node: " + connectionsSettings.getMinimumConnectionsPerNode());
        System.out.println("  Max connections per node: " + connectionsSettings.getMaximumConnectionsPerNode());
        System.out.println("  Max socket idle time: " + connectionsSettings.getMaximumSocketIdleTime());
        System.out.println();
        
        Settings circuitBreakerSettings = testBehavior.getSettings(
            Behavior.OpKind.SYSTEM_CIRCUIT_BREAKER, 
            Behavior.OpShape.SYSTEM, 
            Behavior.Mode.ANY
        );
        System.out.println("Circuit Breaker Settings:");
        System.out.println("  Num tend intervals in error window: " + circuitBreakerSettings.getNumTendIntervalsInErrorWindow());
        System.out.println("  Max errors in error window: " + circuitBreakerSettings.getMaximumErrorsInErrorWindow());
        System.out.println();
        
        Settings txnVerifySettings = testBehavior.getSettings(
            Behavior.OpKind.SYSTEM_TXN_VERIFY, 
            Behavior.OpShape.SYSTEM, 
            Behavior.Mode.ANY
        );
        System.out.println("Transaction Verify Settings:");
        System.out.println("  Consistency: " + txnVerifySettings.getReadModeSC());
        System.out.println("  Replica order: " + txnVerifySettings.getReplicaOrder());
        System.out.println("  Max attempts: " + txnVerifySettings.getMaximumNumberOfCallAttempts());
        System.out.println();

        System.out.println("All examples completed successfully!");
    }
}

