package com.spotify.gradle.bazel;

import com.spotify.gradle.bazel.utils.BazelExecHelper;
import com.spotify.gradle.hatchej.HatchejImlActionFactory;

import java.util.Collections;

import javax.inject.Inject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TestableBazelLeafPlugin extends BazelLeafPlugin {
    @Inject
    public TestableBazelLeafPlugin() throws Exception {
        super(mock(HatchejImlActionFactory.class), mock(BazelAspectServiceFactory.class), mock(BazelExecHelper.class));
        BazelExecHelper.BazelExec versionResult = mock(BazelExecHelper.BazelExec.class);
        doReturn(new BazelExecHelper.RunResult(0, Collections.singletonList("build test"))).when(versionResult).start();
        doReturn(versionResult).when(mBazelExecHelper).createBazelRun(anyBoolean(), anyString(), any(), eq("version"), anyList());
    }
}
