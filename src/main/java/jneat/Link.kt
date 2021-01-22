package jneat

/**
 * Link is a connection from one node to another with an associated weight; It can be marked as recurrent;
 * Its parameters are made public for efficiency.
 */
class Link {
    var weight: Double

    var in_node: NNode

    var out_node: NNode

    var is_recurrent: Boolean

    /**
     * Points to a trait of parameters for genetic creation.
     * Is  link-related parameters that change during Hebbian type learning.
     */
    var linktrait: Trait?

    var added_weight: Double

    /**
     * Is  link-related parameters that change during Hebbian type learning.
     */
    var params = DoubleArray(Neat.p_num_trait_params)

    /**
     * Insert the method's description here.
     * Creation date: (12/01/2002 10.41.28)
     *
     * @param lt    jneat.Trait
     * @param w     double
     * @param inode jneat.NNode
     * @param onode jneat.NNode
     * @param recur boolean
     */
    constructor(lt: Trait?, w: Double, inode: NNode, onode: NNode, recur: Boolean) {
        weight = w
        in_node = inode
        out_node = onode
        is_recurrent = recur
        added_weight = 0.0
        linktrait = lt
    }

    /**
     * Insert the method's description here.
     * Creation date: (15/01/2002 7.53.27)
     *
     * @param c int
     */
    constructor(w: Double, inode: NNode, onode: NNode, recur: Boolean) {
        weight = w
        in_node = inode
        out_node = onode
        is_recurrent = recur
        added_weight = 0.0
        linktrait = null
    }

    /**
     * Insert the method's description here.
     * Creation date: (15/01/2002 8.05.44)
     */
    fun derive_trait(t: Trait?) {
        if (t != null) {
            for (count in 0 until Neat.p_num_trait_params) params[count] = t.getParams(count)
        } else {
            for (count in 0 until Neat.p_num_trait_params) params[count] = 0.0
        }
    }

    fun viewtext() {
        print("\n +LINK : ")
        print("weight=$weight")
        print(", weight-add=$added_weight")
        print(", i(" + in_node?.node_id)
        print(")--<CONNECTION>--o(")
        print(out_node?.node_id?.toString() + ")")
        print(", recurrent=$is_recurrent")
        if (linktrait != null) linktrait!!.viewtext("\n         (linktrait)-> ") else print("\n         *warning* linktrait for this gene is null ")
    }
    /**
     * Insert the method's description here.
     * Creation date: (18/01/2002 9.16.45)
     *
     * @param newIs_traversed boolean
     */
    /**
     * this is a flag for compute depth; if TRUE the connection(link) is already analyzed; FALSE otherwise;
     */
    @JvmField
    var is_traversed = false
}