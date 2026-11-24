package com.coreplatform.authorization.policy;

import java.util.List;

public interface CombiningStrategy {

    PolicyResult combine(List<PolicyResult> results);
}
