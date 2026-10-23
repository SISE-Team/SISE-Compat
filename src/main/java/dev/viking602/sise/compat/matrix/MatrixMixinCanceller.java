package dev.viking602.sise.compat.matrix;
import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
public final class MatrixMixinCanceller {
    public static void register(){MixinCancellerRegistrar.register((targets,name)->name.equals("com.extendedae_plus.mixin.extendedae.common.matrix.ClusterAssemblerMatrixMixin"));}
    private MatrixMixinCanceller(){}
}
