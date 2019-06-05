/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.apache.mxnet.engine;

import com.amazon.ai.Context;
import com.amazon.ai.Model;
import com.amazon.ai.Profiler;
import com.amazon.ai.Translator;
import com.amazon.ai.engine.Engine;
import com.amazon.ai.inference.Predictor;
import com.amazon.ai.nn.NNIndex;
import com.amazon.ai.training.Trainer;
import java.io.File;
import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.mxnet.jna.JnaUtils;
import org.apache.mxnet.nn.MxNNIndex;

public class MxEngine extends Engine {

    public static final NNIndex NN_INDEX = new MxNNIndex();

    MxEngine() {}

    /** {@inheritDoc} */
    @Override
    public String getEngineName() {
        return "MXNet";
    }

    /** {@inheritDoc} */
    @Override
    public int getGpuCount() {
        return JnaUtils.getGpuCount();
    }

    /** {@inheritDoc} */
    @Override
    public MemoryUsage getGpuMemory(Context context) {
        long[] mem = JnaUtils.getGpuMemory(context);
        long committed = mem[1] - mem[0];
        return new MemoryUsage(-1, committed, committed, mem[1]);
    }

    /** {@inheritDoc} */
    @Override
    public Context defaultContext() {
        if (getGpuCount() > 0) {
            return Context.gpu(0);
        }
        return Context.cpu();
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        int version = JnaUtils.getVersion();
        int major = version / 10000;
        int minor = version / 100 - major * 100;
        int patch = version % 100;

        return major + "." + minor + '.' + patch;
    }

    /** {@inheritDoc} */
    @Override
    public Model loadModel(File modelPath, String modelName, int epoch) throws IOException {
        File modelDir;
        if (modelPath.isDirectory()) {
            modelDir = modelPath;
        } else {
            modelDir = modelPath.getParentFile();
        }
        String modelPrefix = new File(modelDir, modelName).getAbsolutePath();
        if (epoch == -1) {
            final Pattern pattern = Pattern.compile(Pattern.quote(modelName) + "-(\\d{4}).params");
            List<Integer> checkpoints =
                    Files.walk(modelDir.toPath(), 1)
                            .map(
                                    p -> {
                                        Matcher m = pattern.matcher(p.toFile().getName());
                                        if (m.matches()) {
                                            return Integer.parseInt(m.group(1));
                                        }
                                        return null;
                                    })
                            .filter(Objects::nonNull)
                            .sorted()
                            .collect(Collectors.toList());
            epoch = checkpoints.get(checkpoints.size() - 1);
        }

        return MxModel.loadModel(modelPrefix, epoch);
    }

    /** {@inheritDoc} */
    @Override
    public NNIndex getNNIndex() {
        return NN_INDEX;
    }

    /** {@inheritDoc} */
    @Override
    public <I, O> Predictor<I, O> newPredictor(
            Model model, Translator<I, O> translator, Context context) {
        return new MxPredictor<>((MxModel) model, translator, context);
    }

    /** {@inheritDoc} */
    @Override
    public Trainer newTrainer(Model model, Context context) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setProfiler(Profiler profiler) {}
}
