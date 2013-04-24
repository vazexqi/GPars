/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  @author who were the original authors?
 *
 *   @author Russel Winder.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import grails.doc.PdfBuilder

class PdfGuide extends DefaultTask {

    @OutputDirectory
    @Input File outputDirectory
    @Input String pdfName

    public PdfGuide() {
        super()
    }

    @TaskAction
    def publish() {
        try {
            PdfBuilder.build(
                    basedir: outputDirectory.absolutePath,
                    home: project.file('grails-doc').absolutePath,
                    tool: 'pdf/gpars'
            )
        } catch (ignore) {
            // it's very likely that the stream is closed before
            // the renderer 'finishes' but it actually does
            // ignore for now
        }
        project.file(outputDirectory.absolutePath + '/guide/single.pdf')
                .renameTo(new File(outputDirectory, pdfName).absolutePath)
    }
}
