package org.fd;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.EnhancedCapability;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ai.Ai;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.persistence.PersistedObject;
import burp.api.montoya.persistence.Preferences;
import burp.api.montoya.ui.menu.Menu;
import burp.api.montoya.ui.menu.MenuItem;

import javax.swing.*;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static burp.api.montoya.EnhancedCapability.AI_FEATURES;

public class AiReporter implements BurpExtension {

    MontoyaApi api;
    Ai ai;
    Logging logging;
    //AiEngine aiEngine;
    LlmClient llmClient;

    private AiReporterUI tab;
    private LlmClient activeClient;
    private Prompt prompts;
    private Preferences preferences;

    private AiReporterContextProvider customContextMenuItemProvider;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    boolean debug = false; //Debug variable

    @Override
    public void initialize(MontoyaApi api) {

        // Save a reference to the MontoyaApi object
        this.api = api;

        // Save a reference to the AI object
        this.ai = api.ai();

        // api.logging() returns an object that we can use to print messages to stdout and stderr
        this.logging = api.logging();

        this.preferences = this.api.persistence().preferences();
        this.prompts = new Prompt(this.preferences);

        // Build the initial client based on saved preference (default: BurpAI)
        this.activeClient = createClient(loadSavedProvider());

        tab = new AiReporterUI(activeClient, executor, this.api);
        tab.setOnProviderChanged(this::switchProvider);
        api.userInterface().registerSuiteTab("AI Reporter", tab.getUI());

        api.extension().setName("AI Reporter");

        // Check if AI is enabled
        if(this.ai.isEnabled())
            this.logging.logToOutput("[AI Reporter] Burp AI enabled!");
        else
            this.logging.logToError("[AI Reporter] Burp AI NOT enabled!");

        PersistedObject persistence = api.persistence().extensionData();
        MarkdownExporter exporter = new MarkdownExporter(api.userInterface().swingUtils().suiteFrame(), persistence);

        // Register our Context Menu Item Provider
        customContextMenuItemProvider = new AiReporterContextProvider(api,activeClient,
                this.debug, exporter, executor);
        api.userInterface().registerContextMenuItemsProvider(customContextMenuItemProvider);

        // Menu bar
        MenuItem editPromptItem = MenuItem.basicMenuItem("Edit prompts")
                .withAction(() -> new PromptDialog(
                        api.userInterface().swingUtils().suiteFrame(), prompts, api
                ).show());

        MenuItem resetPromptsItem = MenuItem.basicMenuItem("Reset all prompts to default")
                .withAction(() -> {
                    int confirm = JOptionPane.showConfirmDialog(api.userInterface().swingUtils().suiteFrame(),
                            "Restore all prompts to their default values?",
                            "Restore Defaults", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        prompts.resetToDefaults();
                    }
                });

        MenuItem exportOptionsItem = MenuItem.basicMenuItem("Change export options")
                .withAction(exporter::showExportOptionsDialog);

        Menu menu = Menu.menu("AI Reporter").withMenuItems(
                editPromptItem, resetPromptsItem, exportOptionsItem);
        api.userInterface().menuBar().registerMenu(menu);

        // Cleanup on unload
        api.extension().registerUnloadingHandler(() -> {
            logging.logToOutput("[AI Reporter] Unloading, shutting down executor...");
            executor.shutdownNow();
        });

        logging.logToOutput("[AI Reporter] Loaded successfully.");
        logging.logToOutput("[AI Reporter] Provider: " + loadSavedProvider());
        logging.logToOutput("[AI Reporter] Endpoint: " + activeClient.getBaseUrl());
        logging.logToOutput("[AI Reporter] Model: " + activeClient.getModel());

    }

    @Override
    public Set<EnhancedCapability> enhancedCapabilities()  {
        return Set.of(AI_FEATURES);
    }

    private LlmClient createClient(LlmProvider provider) {
        return switch (provider) {
            case BURP_AI -> new BurpAiClient(api, prompts, debug);
            case OPENAI_COMPATIBLE -> new OllamaClient(api, prompts, debug);
        };
    }

    private LlmProvider loadSavedProvider() {
        String saved = this.preferences.getString(Prefs.KEY_PROVIDER);
        if (saved != null) {
            try {
                return LlmProvider.valueOf(saved);
            } catch (IllegalArgumentException ignored) { /* fall through */ }
        }

        // By default return Burp AI for PRO and OPENAI_COMPATIBLE for COMMUNITY
        if(api.burpSuite().version().edition() == BurpSuiteEdition.COMMUNITY_EDITION)
            return LlmProvider.OPENAI_COMPATIBLE;
        else
            return LlmProvider.BURP_AI;

    }

    private void switchProvider(LlmProvider provider) {
        logging.logToOutput("[AI Reporter] Switching provider to: " + provider);
        activeClient = createClient(provider);
        tab.setClient(activeClient);
        customContextMenuItemProvider.setClient(activeClient);
        preferences.setString(Prefs.KEY_PROVIDER, provider.name());
    }

}

