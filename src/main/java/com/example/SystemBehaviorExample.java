package com.example;

import java.time.Duration;

import com.aerospike.ClusterDefinition;
import com.aerospike.SystemSettings;
import com.aerospike.SystemSettingsRegistry;
import com.aerospike.client.policy.ReadModeSC;
import com.aerospike.client.policy.Replica;
import com.aerospike.policy.Behavior;
import com.aerospike.policy.Settings;

/**
 * Demonstrates usage of SystemSettings for configuring system-level settings
 * including connections, circuit breaker, and cluster refresh.
 * 
 * <p><b>Note:</b> System-level settings have been moved from Behavior to SystemSettings.
 * Behaviors now only handle transaction-specific operations (txnVerify, txnRoll).</p>
 */
public class SystemBehaviorExample {

    public static void main(String[] args) {
        // Example 1: Configure transaction verification settings (still in Behavior)
        Behavior customTxnVerify = Behavior.DEFAULT.deriveWithChanges("customTxnVerify", builder -> builder
            .on(Behavior.Selectors.transaction().txnVerify(), ops -> ops
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

        // Example 2: Configure transaction rollback settings (still in Behavior)
        Behavior customTxnRoll = Behavior.DEFAULT.deriveWithChanges("customTxnRoll", builder -> builder
            .on(Behavior.Selectors.transaction().txnRoll(), ops -> ops
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

        // Example 3: Configure system settings (NEW API with lambda)
        SystemSettings customSystemSettings = SystemSettings.builder()
            .connections(ops -> ops
                .minimumConnectionsPerNode(10)
                .maximumConnectionsPerNode(200)
                .maximumSocketIdleTime(Duration.ofSeconds(120))
            )
            .circuitBreaker(ops -> ops
                .numTendIntervalsInErrorWindow(2)
                .maximumErrorsInErrorWindow(50)
            )
            .refresh(ops -> ops
                .tendInterval(Duration.ofSeconds(2))
            )
            .build();
        
        System.out.println("Example 3: System Settings (Connections, Circuit Breaker, Refresh)");
        System.out.println(customSystemSettings);
        System.out.println();

        // Example 4: Apply system settings to a cluster connection (inline lambda)
        System.out.println("Example 4: Inline Lambda Configuration");
        System.out.println("Code example:");
        System.out.println("  new ClusterDefinition(\"localhost\", 3000)");
        System.out.println("      .withSystemSettings(builder -> builder");
        System.out.println("          .connections(ops -> ops");
        System.out.println("              .minimumConnectionsPerNode(150)");
        System.out.println("              .maximumConnectionsPerNode(500)");
        System.out.println("          )");
        System.out.println("          .circuitBreaker(ops -> ops");
        System.out.println("              .maximumErrorsInErrorWindow(200)");
        System.out.println("          )");
        System.out.println("      )");
        System.out.println("      .connect();");
        System.out.println();
        
        // Example 4b: Or use pre-built settings
        System.out.println("Example 4b: Pre-built Settings");
        System.out.println("Code example:");
        System.out.println("  new ClusterDefinition(\"localhost\", 3000)");
        System.out.println("      .withSystemSettings(customSystemSettings)");
        System.out.println("      .connect();");
        System.out.println();

        // Example 5: Using SystemSettingsRegistry for default and cluster-specific settings
        System.out.println("Example 5: SystemSettingsRegistry");
        
        SystemSettingsRegistry registry = SystemSettingsRegistry.getInstance();
        
        // Set default system settings using lambda
        SystemSettings defaultSettings = SystemSettings.builder()
            .connections(ops -> ops
                .minimumConnectionsPerNode(100)
                .maximumConnectionsPerNode(400)
            )
            .build();
        registry.updateDefaultSettings(defaultSettings);
        System.out.println("Default settings updated in registry");
        
        // Set cluster-specific settings using lambda
        SystemSettings prodSettings = SystemSettings.builder()
            .connections(ops -> ops
                .minimumConnectionsPerNode(200)  // Override default
            )
            .build();
        registry.updateClusterSettings("prod", prodSettings);
        System.out.println("Cluster-specific settings for 'prod' updated in registry");
        System.out.println();

        // Example 6: Priority hierarchy demonstration
        System.out.println("Example 6: 4-Level Priority Hierarchy");
        System.out.println("Priority (highest to lowest):");
        System.out.println("  1. YAML cluster-specific settings (e.g., for 'prod' cluster)");
        System.out.println("  2. YAML default settings");
        System.out.println("  3. Code-provided settings (via ClusterDefinition.withSystemSettings())");
        System.out.println("  4. Hard-coded defaults (SystemSettings.DEFAULT)");
        System.out.println();
        
        // Demonstrate effective settings calculation
        SystemSettings codeSettings = SystemSettings.builder()
            .connections(ops -> ops
                .minimumConnectionsPerNode(150)
            )
            .build();
        
        SystemSettings effectiveSettings = registry.getEffectiveSettings("prod", codeSettings);
        System.out.println("Effective settings for 'prod' cluster:");
        System.out.println("  - minConns: " + effectiveSettings.getMinimumConnectionsPerNode() + 
                           " (from YAML cluster-specific)");
        System.out.println("  - maxConns: " + effectiveSettings.getMaximumConnectionsPerNode() + 
                           " (from YAML default)");
        System.out.println();

        // Example 7: YAML Configuration Structure
        System.out.println("Example 7: Unified YAML Configuration");
        System.out.println("behaviors:");
        System.out.println("  - name: \"production\"");
        System.out.println("    parent: \"default\"");
        System.out.println("    batchReads:");
        System.out.println("      maxConcurrentNodes: 16");
        System.out.println();
        System.out.println("system:");
        System.out.println("  default:");
        System.out.println("    connections:");
        System.out.println("      minimumConnectionsPerNode: 100");
        System.out.println("      maximumConnectionsPerNode: 400");
        System.out.println("    circuitBreaker:");
        System.out.println("      numTendIntervalsInErrorWindow: 2");
        System.out.println("      maximumErrorsInErrorWindow: 50");
        System.out.println("    refresh:");
        System.out.println("      tendInterval: 1s");
        System.out.println("  clusters:");
        System.out.println("    prod:");
        System.out.println("      connections:");
        System.out.println("        minimumConnectionsPerNode: 200");
        System.out.println();

        // Example 8: Retrieve transaction settings (still in Behavior)
        System.out.println("Example 8: Retrieving Transaction Settings from Behavior");
        
        Settings txnVerifySettings = customTxnVerify.getSettings(
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
        System.out.println();
        System.out.println("Key Takeaways:");
        System.out.println("  - System settings (connections, circuit breaker, refresh) use SystemSettings API");
        System.out.println("  - Transaction operations (txnVerify, txnRoll) remain in Behavior");
        System.out.println("  - Both behaviors and system settings are in the same YAML file");
        System.out.println("  - SystemSettingsRegistry manages default and cluster-specific settings");
        System.out.println("  - Settings are applied via ClusterDefinition.withSystemSettings()");
    }
}
