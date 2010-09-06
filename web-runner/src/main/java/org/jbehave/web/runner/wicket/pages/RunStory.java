package org.jbehave.web.runner.wicket.pages;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.util.value.ValueMap;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.embedder.StoryRunner;
import org.jbehave.core.parsers.StoryParser;
import org.jbehave.core.reporters.TxtOutput;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.web.runner.context.StoryContext;

import com.google.inject.Inject;

public class RunStory extends Template {

    @Inject
    private StoryRunner storyRunner;
    @Inject
    private Configuration configuration;
    @Inject
    private List<CandidateSteps> steps;

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private StoryContext storyContext = new StoryContext();

    public RunStory() {
        this.configuration = withOutputTo(configuration, this.outputStream);
        setPageTitle("Run Story");
        add(new StoryForm("storyForm"));
    }

    @SuppressWarnings("serial")
    public final class StoryForm extends Form<ValueMap> {
        public StoryForm(final String id) {
            // Construct form with no validation listener
            super(id, new CompoundPropertyModel<ValueMap>(new ValueMap()));
            setMarkupId("storyForm");
            add(new TextArea<String>("input").setType(String.class));
            add(new MultiLineLabel("output", ""));
            add(new Button("runButton"));
        }

        @Override
        public final void onSubmit() {
            String input = (String) getModelObject().get("input");
            storyContext.setInput(input);
            run();
            MultiLineLabel output = (MultiLineLabel) get("output");
            output.setDefaultModelObject(storyContext.getOutput());
        }
    }

    private Configuration withOutputTo(Configuration configuration, OutputStream ouputStream) {
        final Properties outputPatterns = new Properties();
        outputPatterns.setProperty("beforeStory", "{0}\n");
        final Keywords keywords = configuration.keywords();
        final boolean reportFailureTrace = false;
        return configuration.useDefaultStoryReporter(new TxtOutput(new PrintStream(outputStream), outputPatterns,
                keywords, reportFailureTrace));
    }

    public void run() {
        if (isNotBlank(storyContext.getInput())) {
            try {
                outputStream.reset();
                storyContext.clearFailureCause();
                StoryParser storyParser = configuration.storyParser();
                storyRunner.run(configuration, steps, storyParser.parseStory(storyContext.getInput()));
            } catch (Throwable e) {
                storyContext.runFailedFor(e);
            }
            storyContext.setOutput(outputStream.toString());
        }
    }

}
