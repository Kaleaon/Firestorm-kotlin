package com.firestorm.newview

import java.util.UUID
import kotlin.math.abs

class FSLocalMeshImportGLTF : FSLocalMeshImportBase() {

    private var mParentMap: MutableList<Int> = mutableListOf()

    init {
        mLogToInfo = false
    }

    fun loadFile(
        filename: String,
        lod: LocalMeshFileLOD,
        objectVector: MutableList<LLLocalMeshObject>
    ): LoadFileReturn {
        pushLog("GLTF Importer", "Starting")
        setLod(lod)

        // no-op: load glTF asset from filename not yet implemented
        System.err.println("FSLocalMeshImportGLTF: load glTF asset from filename not yet implemented")
        val asset: GltfAsset? = null
        if (asset == null) return LoadFileReturn(false, mLoadingLog)

        mParentMap = buildParentMap(asset)

        if (asset.scenes.isEmpty()) {
            pushLog("GLTF Importer", "GLTF asset contains no scenes.")
            return LoadFileReturn(false, mLoadingLog)
        }

        val sceneIdx = if (asset.activeScene >= 0 && asset.activeScene < asset.scenes.size) asset.activeScene else 0
        val scene = asset.scenes[sceneIdx]

        // no-op: scene.updateTransforms(asset) — update node matrices without GL upload

        val meshNodes = mutableListOf<Int>()
        for (rootIdx in scene.nodes) {
            collectMeshNodes(asset, rootIdx, meshNodes)
        }

        if (meshNodes.isEmpty()) {
            pushLog("GLTF Importer", "GLTF asset contains no mesh nodes.")
            return LoadFileReturn(false, mLoadingLog)
        }

        for ((objectIdx, nodeIdx) in meshNodes.withIndex()) {
            val node = asset.nodes[nodeIdx]

            if (node.meshIndex < 0 || node.meshIndex >= asset.meshes.size) {
                pushLog("GLTF Importer", "Mesh index out of bounds for node $nodeIdx, skipping.")
                continue
            }

            var objectName = node.name
            if (objectName.isEmpty()) objectName = asset.meshes[node.meshIndex].name
            if (objectName.isEmpty()) objectName = "node_$nodeIdx"

            if (mLod == LocalMeshFileLOD.LOCAL_LOD_HIGH) {
                val currentObject = LLLocalMeshObject(objectName)
                val success = processNodeMesh(asset, node, currentObject)
                if (success) {
                    pushLog("GLTF Importer", "Object loaded successfully.")
                    val identityTransform = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f }
                    postProcessObject(currentObject, identityTransform, true)
                    objectVector.add(currentObject)
                } else {
                    pushLog("GLTF Importer", "Object loading failed, skipping.")
                }
            } else {
                if (objectVector.size <= objectIdx) {
                    pushLog("GLTF Importer", "LOD$mLod is requesting an object that LOD3 did not have or failed to load, skipping.")
                    continue
                }
                val currentObject = objectVector[objectIdx]
                val success = processNodeMesh(asset, node, currentObject)
                if (success) {
                    pushLog("GLTF Importer", "Object loaded successfully.")
                    val identityTransform = FloatArray(16).also { it[0] = 1f; it[5] = 1f; it[10] = 1f; it[15] = 1f }
                    postProcessObject(currentObject, identityTransform, false)
                } else {
                    pushLog("GLTF Importer", "Object loading failed.")
                }
            }
        }

        if (objectVector.isEmpty()) {
            pushLog("GLTF Importer", "No objects have been successfully loaded, stopping.")
            return LoadFileReturn(false, mLoadingLog)
        }

        if (mLod == LocalMeshFileLOD.LOCAL_LOD_HIGH) {
            for (obj in objectVector) {
                finalizeSkinInfo(obj)
            }
        }

        pushLog("GLTF Importer", "Object and face parsing complete.")
        return LoadFileReturn(true, mLoadingLog)
    }

    private fun processNodeMesh(asset: GltfAsset, node: GltfNode, obj: LLLocalMeshObject): Boolean {
        if (node.meshIndex < 0 || node.meshIndex >= asset.meshes.size) {
            pushLog("GLTF Importer", "Invalid mesh index for node.")
            return false
        }

        val mesh = asset.meshes[node.meshIndex]
        if (mesh.primitives.isEmpty()) {
            pushLog("GLTF Importer", "Mesh has no primitives, skipping.")
            return false
        }

        val objectFaces = obj.getFaces(mLod)
        objectFaces.clear()

        var skinIdx = node.skinIndex
        var applyXyRotation = false
        var jointIndexRemap = listOf<Int>()
        var skinJointNames = listOf<String>()

        if (skinIdx >= 0 && skinIdx < asset.skins.size) {
            val jointMap = loadJointMap()
            val skin = asset.skins[skinIdx]

            applyXyRotation = checkForXYRotation(asset, skin, mParentMap, jointMap)

            skinJointNames = skin.joints.map { jIdx ->
                val rawName = if (jIdx >= 0 && jIdx < asset.nodes.size) asset.nodes[jIdx].name else ""
                normalizeJointName(jointMap, rawName)
            }

            val canonicalSkin = obj.getObjectMeshSkinInfo()
            if (mLod == LocalMeshFileLOD.LOCAL_LOD_HIGH || canonicalSkin == null || canonicalSkin.jointNames.isEmpty()) {
                if (!initSkinInfo(asset, skinIdx, obj)) {
                    skinIdx = -1
                }
            }

            if (skinIdx >= 0) {
                val updatedCanonical = obj.getObjectMeshSkinInfo()
                jointIndexRemap = buildJointIndexRemap(asset, skin, jointMap, updatedCanonical)
            }
        } else {
            skinIdx = -1
        }

        val nodeIdx = asset.nodes.indexOf(node)
        val meshTransform: FloatArray = run {
            val combined = FloatArray(16)
            computeCombinedNodeTransform(asset, mParentMap, nodeIdx, combined)
            // no-op: multiply kCoordSystemRotation * combined, and if applyXyRotation then kCoordSystemRotationXY * that
            combined
        }

        val flipWinding: Boolean = false // no-op: glm::determinant(meshTransform) < 0

        var submeshFailureFound = false
        var stopLoadingAdditionalFaces = false

        for (prim in mesh.primitives) {
            if (stopLoadingAdditionalFaces) break

            if (objectFaces.size >= LL_SCULPT_MESH_MAX_FACES) {
                pushLog("GLTF Importer", "NOTE: reached the limit of $LL_SCULPT_MESH_MAX_FACES faces per object, ignoring the rest.")
                stopLoadingAdditionalFaces = true
                break
            }

            val faceOk = appendPrimitiveToObject(asset, prim, meshTransform, flipWinding, jointIndexRemap, skinJointNames, obj, skinIdx)
            if (!faceOk) submeshFailureFound = true
        }

        return !submeshFailureFound
    }

    private fun appendPrimitiveToObject(
        asset: GltfAsset,
        prim: GltfPrimitive,
        meshTransform: FloatArray,
        flipWinding: Boolean,
        jointIndexRemap: List<Int>,
        skinJointNames: List<String>,
        obj: LLLocalMeshObject,
        skinIdx: Int
    ): Boolean {
        val submesh = LLLocalMeshFace()

        if (prim.positions.isEmpty()) {
            pushLog("GLTF Importer", "Primitive has no positions, skipping.")
            return false
        }

        val (triangleIndices, triangleError) = buildTriangleIndexArray(prim)
        if (triangleIndices == null) {
            pushLog("GLTF Importer", "Primitive triangulation failed: $triangleError")
            return false
        }

        for (idx in triangleIndices) {
            if (idx >= prim.positions.size) {
                pushLog("GLTF Importer", "Primitive index out of bounds, skipping.")
                return false
            }
        }

        val normalTransform: FloatArray = identityMatrix4x4() // no-op: transpose(inverse(mat3(meshTransform)))

        val listPositions = submesh.getPositions()
        val listNormals = submesh.getNormals()
        val listUvs = submesh.getUVs()
        val listIndices = submesh.getIndices()

        for (vertIdx in prim.positions.indices) {
            val pos = prim.positions[vertIdx]
            val transformedPos: FloatArray = pos.copyOf() // no-op: meshTransform * vec4(pos[0], pos[1], pos[2], 1)
            listPositions.add(transformedPos)

            if (vertIdx == 0) submesh.setFaceBoundingBox(transformedPos, true)
            else submesh.setFaceBoundingBox(transformedPos)

            if (prim.normals.isNotEmpty() && vertIdx < prim.normals.size) {
                val norm = prim.normals[vertIdx]
                val transformedNorm: FloatArray = norm.copyOf() // no-op: normalTransform * vec3(norm[0], norm[1], norm[2])
                listNormals.add(transformedNorm)
            } else {
                listNormals.add(floatArrayOf(0f, 0f, 1f, 0f))
            }

            if (prim.texCoords0.isNotEmpty() && vertIdx < prim.texCoords0.size) {
                val uv = prim.texCoords0[vertIdx]
                listUvs.add(floatArrayOf(uv[0], -uv[1]))
            }
        }

        var idx = 0
        while (idx < triangleIndices.size) {
            listIndices.add(triangleIndices[idx])
            listIndices.add(triangleIndices[idx + if (flipWinding) 2 else 1])
            listIndices.add(triangleIndices[idx + if (flipWinding) 1 else 2])
            idx += 3
        }

        if (skinIdx >= 0 && skinIdx < asset.skins.size
            && prim.weights.isNotEmpty() && prim.joints.isNotEmpty()
            && prim.weights.size == prim.positions.size
            && prim.joints.size == prim.positions.size
        ) {
            val skin = asset.skins[skinIdx]
            val jointComponentTypeIsUShort: Boolean = false // no-op: check JOINTS_0 accessor component type == UNSIGNED_SHORT

            val listSkin = submesh.getSkin()
            var droppedWeightedInfluences = 0
            var affectedVertices = 0
            val droppedJointNames = mutableSetOf<String>()

            for (vertIdx in prim.weights.indices) {
                val weightVec = prim.weights[vertIdx]
                val rawJointIndices: IntArray = if (jointComponentTypeIsUShort) {
                    IntArray(4) // no-op: unpack prim.joints[vertIdx] as u16vec4
                } else {
                    IntArray(4) // no-op: unpack prim.joints[vertIdx] as u8vec4
                }

                val weightValues = floatArrayOf(weightVec[0], weightVec[1], weightVec[2], weightVec[3])
                val total = weightValues.sum()
                if (total > 0f) for (j in weightValues.indices) weightValues[j] /= total

                val unit = LLLocalMeshFace.LLLocalMeshSkinUnit()
                var vertexDroppedJoint = false

                for (j in 0 until 4) {
                    var remappedJoint = rawJointIndices[j]
                    if (jointIndexRemap.isNotEmpty()) {
                        remappedJoint = if (remappedJoint < 0 || remappedJoint >= jointIndexRemap.size) -1
                        else jointIndexRemap[remappedJoint]
                    }

                    if (weightValues[j] > 0f && remappedJoint < 0) {
                        ++droppedWeightedInfluences
                        vertexDroppedJoint = true
                        if (rawJointIndices[j] >= 0 && rawJointIndices[j] < skinJointNames.size) {
                            droppedJointNames.add(skinJointNames[rawJointIndices[j]])
                        }
                    }

                    if (remappedJoint < 0 || weightValues[j] <= 0f) {
                        unit.jointIndices[j] = -1
                        unit.jointWeights[j] = 0f
                    } else {
                        unit.jointIndices[j] = remappedJoint
                        unit.jointWeights[j] = weightValues[j].coerceIn(0f, 0.999f)
                    }
                }

                if (vertexDroppedJoint) ++affectedVertices
                listSkin.add(unit)
            }

            if (droppedWeightedInfluences > 0) {
                val jointStr = droppedJointNames.sorted().joinToString(", ")
                var warning = "LOD$mLod object \"${obj.getObjectName()}\" dropped $droppedWeightedInfluences" +
                    " weighted joint influence(s) across $affectedVertices vertex/vertices while remapping to the high LOD skin"
                if (droppedJointNames.isNotEmpty()) warning += " [$jointStr]"
                pushLog("GLTF Importer", "WARNING: $warning")
            }
        } else if (skinIdx >= 0) {
            pushLog("GLTF Importer", "Skinning data missing or mismatched for primitive, skipping weights.")
        }

        obj.getFaces(mLod).add(submesh)
        return true
    }

    private fun initSkinInfo(asset: GltfAsset, skinIdx: Int, obj: LLLocalMeshObject): Boolean {
        if (skinIdx < 0 || skinIdx >= asset.skins.size) return false

        val skinInfo = obj.getObjectMeshSkinInfo() ?: MeshSkinInfo()

        val jointMap = loadJointMap()
        val skin = asset.skins[skinIdx]
        val applyXyRotation = checkForXYRotation(asset, skin, mParentMap, jointMap)
        val isSkinJoint = buildSkinJointMembership(asset, skin)

        var recognizedJointCount = 0u
        for (jointNodeIdx in skin.joints) {
            if (jointNodeIdx >= 0 && jointNodeIdx < asset.nodes.size
                && jointMap.containsKey(asset.nodes[jointNodeIdx].name)
            ) {
                ++recognizedJointCount
            }
        }

        val jointsData: MutableMap<Int, JointNodeData> = mutableMapOf()
        val namesToNodes: MutableMap<String, Int> = mutableMapOf()
        // no-op: gAgentAvatarp.getJointMatricesAndHierarchy() not yet implemented
        System.err.println("FSLocalMeshImportGLTF: gAgentAvatarp.getJointMatricesAndHierarchy() not yet implemented")
        val viewerSkeleton: List<LLJointData> = emptyList()

        val canBuildOverrides = viewerSkeleton.isNotEmpty()
        if (canBuildOverrides) {
            for (i in skin.joints.indices) {
                val jointNodeIdx = skin.joints[i]
                if (jointNodeIdx < 0 || jointNodeIdx >= asset.nodes.size) continue

                val jointNode = asset.nodes[jointNodeIdx]
                val data = JointNodeData()
                data.nodeIdx = jointNodeIdx
                data.gltfRestMatrix = buildGltfRestMatrix(asset, mParentMap, isSkinJoint, jointNodeIdx)
                data.gltfMatrix = jointNode.matrix.copyOf()
                data.overrideMatrix = identityMatrix4x4()

                val nameIt = jointMap[jointNode.name]
                if (nameIt != null) {
                    data.name = nameIt
                    data.isValidViewerJoint = true
                } else {
                    data.name = jointNode.name
                    data.isValidViewerJoint = false
                }

                jointsData[jointNodeIdx] = data
                namesToNodes[data.name] = jointNodeIdx

                for (childIdx in jointNode.children) {
                    val childData = jointsData.getOrPut(childIdx) { JointNodeData() }
                    childData.parentNodeIdx = jointNodeIdx
                    childData.isParentValidViewerJoint = data.isValidViewerJoint
                    jointsData[childIdx] = childData
                }
            }

            val identity = identityMatrix4x4()
            for (viewerData in viewerSkeleton) {
                buildOverrideMatrix(viewerData, jointsData, namesToNodes, identity, identity, applyXyRotation)
            }
        }

        if (!enforceRigJointLimit("GLTF Importer", obj, skinInfo, recognizedJointCount)) return false

        skinInfo.bindShapeMatrix = identityMatrix4x4()
        skinInfo.bindPoseMatrix.clear()
        skinInfo.jointNames.clear()
        skinInfo.jointNums.clear()
        skinInfo.invBindMatrix.clear()
        skinInfo.alternateBindMatrix.clear()
        skinInfo.invalidJointsScrubbed = false
        skinInfo.jointNumsInitialized = false

        // no-op: read FSLocalMeshApplyJointOffsets setting not yet implemented
        System.err.println("FSLocalMeshImportGLTF: read FSLocalMeshApplyJointOffsets setting not yet implemented")
        val applyJointOffsets: Boolean = false

        for (i in skin.joints.indices) {
            val jointNodeIdx = skin.joints[i]
            var jointName = if (jointNodeIdx >= 0 && jointNodeIdx < asset.nodes.size) asset.nodes[jointNodeIdx].name else ""
            jointName = normalizeJointName(jointMap, jointName)
            skinInfo.jointNames.add(jointName)
            skinInfo.jointNums.add(-1)

            val originalBind: FloatArray = if (i < skin.inverseBindMatricesData.size) {
                identityMatrix4x4() // no-op: inverse(skin.inverseBindMatricesData[i])
            } else {
                identityMatrix4x4()
            }

            val rotatedBind: FloatArray = originalBind.copyOf() // no-op: convertTransformToViewerBasis(originalBind, applyXyRotation)
            val skeletonTransform: FloatArray = if (canBuildOverrides) {
                computeGltfToViewerSkeletonTransform(jointsData, jointNodeIdx, applyXyRotation)
            } else {
                identityMatrix4x4()
            }

            val translatedBind: FloatArray = rotatedBind.copyOf() // no-op: skeletonTransform * rotatedBind
            val finalInverseBind: FloatArray = translatedBind.copyOf() // no-op: inverse(translatedBind)
            skinInfo.invBindMatrix.add(finalInverseBind)

            if (applyJointOffsets && canBuildOverrides) {
                val alternateBind = finalInverseBind.copyOf()
                val jointIt = jointsData[jointNodeIdx]
                if (jointIt != null) {
                    // no-op: set translation of alternateBind from jointIt.overrideMatrix translation
                }
                skinInfo.alternateBindMatrix.add(alternateBind)
            }
        }

        obj.setObjectMeshSkinInfo(skinInfo)
        return true
    }

    private fun finalizeSkinInfo(obj: LLLocalMeshObject) {
        val skinInfo = obj.getObjectMeshSkinInfo() ?: return
        if (skinInfo.invBindMatrix.isEmpty()) return

        val normalizedTransformation = buildNormalizedTransformation(obj)

        val bindShapePtr = skinInfo.bindShapeMatrix
        val bindShapeFinite = bindShapePtr.all { it.isFinite() }
        if (!bindShapeFinite) {
            skinInfo.bindShapeMatrix = identityMatrix4x4()
        }

        // no-op: matMul(normalizedTransformation, skinInfo.bindShapeMatrix) → skinInfo.bindShapeMatrix

        buildBindPoseMatrix(skinInfo)
        skinInfo.updateHash()
        obj.setObjectMeshSkinInfo(skinInfo)
    }

    private inner class JointNodeData {
        var nodeIdx: Int = -1
        var parentNodeIdx: Int = -1
        var gltfRestMatrix: FloatArray = identityMatrix4x4()
        var viewerRestMatrix: FloatArray = identityMatrix4x4()
        var overrideRestMatrix: FloatArray = identityMatrix4x4()
        var gltfMatrix: FloatArray = identityMatrix4x4()
        var overrideMatrix: FloatArray = identityMatrix4x4()
        var name: String = ""
        var isValidViewerJoint: Boolean = false
        var isParentValidViewerJoint: Boolean = false
        var isOverrideValid: Boolean = false
    }

    private fun buildParentMap(asset: GltfAsset): MutableList<Int> {
        val parentMap = MutableList(asset.nodes.size) { -1 }
        for (i in asset.nodes.indices) {
            for (childIdx in asset.nodes[i].children) {
                if (childIdx >= 0 && childIdx < parentMap.size) {
                    parentMap[childIdx] = i
                }
            }
        }
        return parentMap
    }

    private fun buildSkinJointMembership(asset: GltfAsset, skin: GltfSkin): List<Boolean> {
        val result = MutableList(asset.nodes.size) { false }
        for (jointNodeIdx in skin.joints) {
            if (jointNodeIdx >= 0 && jointNodeIdx < result.size) result[jointNodeIdx] = true
        }
        return result
    }

    private fun collectMeshNodes(asset: GltfAsset, nodeIdx: Int, meshNodes: MutableList<Int>) {
        if (nodeIdx < 0 || nodeIdx >= asset.nodes.size) return
        val node = asset.nodes[nodeIdx]
        if (node.meshIndex >= 0) meshNodes.add(nodeIdx)
        for (childIdx in node.children) collectMeshNodes(asset, childIdx, meshNodes)
    }

    private fun computeCombinedNodeTransform(asset: GltfAsset, parentMap: List<Int>, nodeIndex: Int, combined: FloatArray) {
        if (nodeIndex < 0 || nodeIndex >= asset.nodes.size) {
            val identity = identityMatrix4x4()
            identity.copyInto(combined)
            return
        }
        // no-op: accumulate node.matrix up the parent chain into combined
        System.err.println("FSLocalMeshImportGLTF: accumulate node.matrix up the parent chain into combined not yet implemented")
    }

    private fun normalizeJointName(jointMap: Map<String, String>, name: String): String =
        jointMap[name] ?: name

    private fun buildJointIndexRemap(
        asset: GltfAsset,
        skin: GltfSkin,
        jointMap: Map<String, String>,
        canonicalSkin: MeshSkinInfo?
    ): List<Int> {
        val remap = MutableList(skin.joints.size) { -1 }
        if (canonicalSkin == null || canonicalSkin.jointNames.isEmpty()) {
            return remap.mapIndexed { i, _ -> i }
        }
        val canonicalIndices = canonicalSkin.jointNames.mapIndexed { i, n -> n to i }.toMap()
        for (i in skin.joints.indices) {
            val jointNodeIdx = skin.joints[i]
            if (jointNodeIdx < 0 || jointNodeIdx >= asset.nodes.size) continue
            val jointName = normalizeJointName(jointMap, asset.nodes[jointNodeIdx].name)
            canonicalIndices[jointName]?.let { remap[i] = it }
        }
        return remap
    }

    private fun buildTriangleIndexArray(prim: GltfPrimitive): Pair<List<Int>?, String> {
        val baseIndices: List<Int> = if (prim.indexArray.isNotEmpty()) prim.indexArray
        else prim.positions.indices.toList()

        return when (prim.mode) {
            GltfPrimitiveMode.TRIANGLES -> {
                if (baseIndices.size % 3 != 0) return null to "Index count is not divisible by 3."
                baseIndices to ""
            }
            GltfPrimitiveMode.TRIANGLE_STRIP -> {
                if (baseIndices.size < 3) return null to "Triangle strip has fewer than 3 indices."
                val out = mutableListOf<Int>()
                for (i in 2 until baseIndices.size) {
                    var i0 = baseIndices[i - 2]; var i1 = baseIndices[i - 1]; val i2 = baseIndices[i]
                    if (i % 2 == 1) { val tmp = i0; i0 = i1; i1 = tmp }
                    out.add(i0); out.add(i1); out.add(i2)
                }
                out to ""
            }
            GltfPrimitiveMode.TRIANGLE_FAN -> {
                if (baseIndices.size < 3) return null to "Triangle fan has fewer than 3 indices."
                val out = mutableListOf<Int>()
                for (i in 2 until baseIndices.size) {
                    out.add(baseIndices[0]); out.add(baseIndices[i - 1]); out.add(baseIndices[i])
                }
                out to ""
            }
            else -> null to "Unsupported primitive mode."
        }
    }

    private fun buildGltfRestMatrix(asset: GltfAsset, parentMap: List<Int>, isSkinJoint: List<Boolean>, jointNodeIndex: Int): FloatArray {
        if (jointNodeIndex < 0 || jointNodeIndex >= asset.nodes.size) return identityMatrix4x4()
        // no-op: accumulate asset.nodes[jointNodeIndex].matrix up the parent chain, stopping when parent is not a skin joint
        System.err.println("FSLocalMeshImportGLTF: accumulate node matrix up parent chain not yet implemented")
        return identityMatrix4x4()
    }

    private fun checkForXYRotation(asset: GltfAsset, skin: GltfSkin, parentMap: List<Int>, jointMap: Map<String, String>): Boolean {
        val isSkinJoint = buildSkinJointMembership(asset, skin)
        var jointsFound = 0
        for (i in skin.joints.indices) {
            if (i >= skin.inverseBindMatricesData.size) continue
            val jointNodeIdx = skin.joints[i]
            if (jointNodeIdx < 0 || jointNodeIdx >= asset.nodes.size) continue
            var jointName = asset.nodes[jointNodeIdx].name
            jointName = jointMap[jointName] ?: continue
            if (jointName != "mShoulderRight" && jointName != "mShoulderLeft") continue

            val gltfJointRest = buildGltfRestMatrix(asset, parentMap, isSkinJoint, jointNodeIdx)
            val isXyRotated: Boolean = false // no-op: inverse(gltfJointRest) * inverseBindMatricesData[i], check diag < 0.5
            if (!isXyRotated) return false
            ++jointsFound
        }
        return jointsFound == 2
    }

    private fun buildOverrideMatrix(
        viewerData: LLJointData,
        gltfNodes: MutableMap<Int, JointNodeData>,
        namesToNodes: MutableMap<String, Int>,
        parentRest: FloatArray,
        parentSupportRest: FloatArray,
        applyXyRotation: Boolean
    ) {
        // no-op: mirror buildOverrideMatrix — decompose gltf rest in viewer basis, extract translation override, propagate down skeleton hierarchy
        System.err.println("FSLocalMeshImportGLTF: buildOverrideMatrix not yet implemented")
    }

    private fun computeGltfToViewerSkeletonTransform(
        jointsDataMap: Map<Int, JointNodeData>,
        gltfNodeIndex: Int,
        applyXyRotation: Boolean
    ): FloatArray {
        val nodeData = jointsDataMap[gltfNodeIndex] ?: return identityMatrix4x4()
        if (!nodeData.isOverrideValid) return identityMatrix4x4()
        // no-op: overrideRestMatrix * inverse(convertTransformToViewerBasis(gltfRestMatrix, applyXyRotation))
        System.err.println("FSLocalMeshImportGLTF: computeGltfToViewerSkeletonTransform not yet implemented")
        return identityMatrix4x4()
    }

    private fun identityMatrix4x4(): FloatArray =
        floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
}

interface GltfAsset {
    val nodes: List<GltfNode>
    val meshes: List<GltfMesh>
    val skins: List<GltfSkin>
    val scenes: List<GltfScene>
    val activeScene: Int
}

interface GltfNode {
    val name: String
    val meshIndex: Int
    val skinIndex: Int
    val children: List<Int>
    val matrix: FloatArray
}

interface GltfMesh {
    val name: String
    val primitives: List<GltfPrimitive>
}

interface GltfPrimitive {
    val positions: List<FloatArray>
    val normals: List<FloatArray>
    val texCoords0: List<FloatArray>
    val weights: List<FloatArray>
    val joints: List<Long>
    val indexArray: List<Int>
    val mode: GltfPrimitiveMode
    val attributes: Map<String, Int>
}

interface GltfSkin {
    val joints: List<Int>
    val inverseBindMatricesData: List<FloatArray>
}

interface GltfScene {
    val nodes: List<Int>
}

enum class GltfPrimitiveMode { TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN, OTHER }
