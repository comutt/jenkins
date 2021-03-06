/*
 * The MIT License
 *
 * Copyright 2013 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.cli;

import static hudson.cli.CLICommandInvoker.Matcher.failedWith;
import static hudson.cli.CLICommandInvoker.Matcher.hasNoStandardOutput;
import static hudson.cli.CLICommandInvoker.Matcher.hasNoErrorOutput;
import static hudson.cli.CLICommandInvoker.Matcher.succeeded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;

import hudson.model.ListView;
import hudson.model.View;
import jenkins.model.Jenkins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CreateViewCommandTest {

    private CLICommandInvoker command;

    @Rule public final JenkinsRule j = new JenkinsRule();

    @Before public void setUp() {

        command = new CLICommandInvoker(j, new CreateViewCommand());
    }

    @Test public void createViewShouldFailWithoutViewCreatePermission() {

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.READ)
                .withStdin(this.getClass().getResourceAsStream("/hudson/cli/view.xml"))
                .invoke()
        ;

        assertThat(result, failedWith(-1));
        assertThat(result, hasNoStandardOutput());
        assertThat(result.stderr(), containsString("user is missing the View/Create permission"));
    }

    @Test public void createViewShouldSucceed() {

        final CLICommandInvoker.Result result = command
                .authorizedTo(View.CREATE, Jenkins.READ)
                .withStdin(this.getClass().getResourceAsStream("/hudson/cli/view.xml"))
                .invoke()
        ;

        assertThat(result, succeeded());
        assertThat(result, hasNoErrorOutput());
        assertThat(result, hasNoStandardOutput());

        final View updatedView = j.jenkins.getView("ViewFromXML");
        assertThat(updatedView.getViewName(), equalTo("ViewFromXML"));
        assertThat(updatedView.isFilterExecutors(), equalTo(true));
        assertThat(updatedView.isFilterQueue(), equalTo(false));
    }

    @Test public void createViewSpecifyingNameExplicitlyShouldSucceed() {

        final CLICommandInvoker.Result result = command
                .authorizedTo(View.CREATE, Jenkins.READ)
                .withStdin(this.getClass().getResourceAsStream("/hudson/cli/view.xml"))
                .invokeWithArgs("CustomViewName")
        ;

        assertThat(result, succeeded());
        assertThat(result, hasNoErrorOutput());
        assertThat(result, hasNoStandardOutput());

        assertThat("A view with original name should not exist", j.jenkins.getView("ViewFromXML"), nullValue());

        final View updatedView = j.jenkins.getView("CustomViewName");
        assertThat(updatedView.getViewName(), equalTo("CustomViewName"));
        assertThat(updatedView.isFilterExecutors(), equalTo(true));
        assertThat(updatedView.isFilterQueue(), equalTo(false));
    }

    @Test public void createViewShouldFailIfViewAlreadyExists() throws IOException {

        j.jenkins.addView(new ListView("ViewFromXML"));

        final CLICommandInvoker.Result result = command
                .authorizedTo(View.CREATE, Jenkins.READ)
                .withStdin(this.getClass().getResourceAsStream("/hudson/cli/view.xml"))
                .invoke()
        ;

        assertThat(result, failedWith(-1));
        assertThat(result, hasNoStandardOutput());
        assertThat(result.stderr(), containsString("View 'ViewFromXML' already exists"));
    }

    @Test public void createViewShouldFailUsingInvalidName() {

        final CLICommandInvoker.Result result = command
                .authorizedTo(View.CREATE, Jenkins.READ)
                .withStdin(this.getClass().getResourceAsStream("/hudson/cli/view.xml"))
                .invokeWithArgs("..")
        ;

        assertThat(result, failedWith(-1));
        assertThat(result, hasNoStandardOutput());
        assertThat(result.stderr(), containsString("Invalid view name"));
    }
}
