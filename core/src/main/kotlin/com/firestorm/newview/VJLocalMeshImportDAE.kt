package com.firestorm.newview

import java.util.UUID

typealias JointTransformMap = MutableMap<String, FloatArray>

class LLLocalMeshImportDAE : FSLocalMeshImportBase() {

    fun loadFile(
        filename: String,
        lod: LocalMeshFileLOD,
        objectVector: MutableList<LLLocalMeshObject>
    ): LoadFileReturn {
        pushLog("DAE Importer", "Starting")
        setLod(lod)

        val colladaCore: DaeHandle = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — instantiate a COLLADA DOM or JAXB-parsed DAE document not yet implemented")
            object : DaeHandle {}
        }
        val preprocessDae: Boolean = run {
            System.err.println("LLLocalMeshImportDAE: read ImporterPreprocessDAE setting not yet implemented")
            false
        }
        val colladaDom: DaeDocument = if (preprocessDae) {
            run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — open DAE from preprocessed string (LLDAELoader::preprocessDAE equivalent) not yet implemented")
                object : DaeDocument {}
            }
        } else {
            run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — open DAE directly from filename not yet implemented")
                object : DaeDocument {}
            }
        }

        val alwaysUseMeterScale: Boolean = run {
            System.err.println("LLLocalMeshImportDAE: read FSLocalMeshScaleAlwaysMeters setting not yet implemented")
            false
        }

        var sceneTransformBase = identityMatrix4x4()
        if (!alwaysUseMeterScale) {
            val meter: Float = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — read domAsset.domUnit meter value from DAE document not yet implemented")
                1f
            }
            sceneTransformBase[0] = meter; sceneTransformBase[5] = meter; sceneTransformBase[10] = meter
        }

        val upAxis: UpAxisType = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — read domAsset.domUp_axis from DAE document, default Y_UP not yet implemented")
            UpAxisType.Z_UP
        }
        val rotation = when (upAxis) {
            UpAxisType.X_UP -> run {
                System.err.println("LLLocalMeshImportDAE: rotation matrix 0, 90°, 0 (Y rotation) not yet implemented")
                identityMatrix4x4()
            }
            UpAxisType.Y_UP -> run {
                System.err.println("LLLocalMeshImportDAE: rotation matrix 90°, 0, 0 (X rotation) not yet implemented")
                identityMatrix4x4()
            }
            UpAxisType.Z_UP -> identityMatrix4x4()
        }

        sceneTransformBase = multiplyMatrix4x4(rotation, sceneTransformBase)

        val meshAmount: Int = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — collada_db.getElementCount(COLLADA_TYPE_MESH) not yet implemented")
            0
        }
        val skinAmount: Int = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — collada_db.getElementCount(COLLADA_TYPE_SKIN) not yet implemented")
            0
        }

        if (meshAmount == 0) {
            pushLog("DAE Importer", "Collada document contained no MESH instances.")
            return LoadFileReturn(false, mLoadingLog)
        }

        val meshUsageTracker: MutableList<DaeMeshHandle> = mutableListOf()

        for (meshIndex in 0 until meshAmount) {
            pushLog("DAE Importer", "Parsing object number $meshIndex")

            val meshCurrent: DaeMeshHandle = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — collada_db.getElement(meshIndex, COLLADA_TYPE_MESH) not yet implemented")
                object : DaeMeshHandle {}
            }
            val objectName = getElementName(meshCurrent, meshIndex)
            pushLog("DAE Importer", "Object name found: $objectName")

            if (mLod == LocalMeshFileLOD.LOCAL_LOD_HIGH) {
                val currentObject = LLLocalMeshObject(objectName)
                val success = processObject(meshCurrent, currentObject)
                if (success) {
                    pushLog("DAE Importer", "Object loaded successfully.")
                    postProcessObject(currentObject, sceneTransformBase, true)
                    objectVector.add(currentObject)
                    meshUsageTracker.add(meshCurrent)
                } else {
                    pushLog("DAE Importer", "Object loading failed, skipping this one.")
                }
            } else {
                if (objectVector.size <= meshIndex) {
                    pushLog("DAE Importer", "LOD$mLod is requesting an object that LOD3 did not have or failed to load, skipping.")
                    continue
                }
                val currentObject = objectVector[meshIndex]
                val success = processObject(meshCurrent, currentObject)
                if (success) {
                    pushLog("DAE Importer", "Object loaded successfully.")
                    postProcessObject(currentObject, sceneTransformBase, false)
                } else {
                    pushLog("DAE Importer", "Object loading failed.")
                }
            }
        }

        if (objectVector.isEmpty()) {
            pushLog("DAE Importer", "No objects have been successfully loaded, stopping.")
            return LoadFileReturn(false, mLoadingLog)
        }

        pushLog("DAE Importer", "Object and face parsing complete.")

        var skinsLoaded = 0
        for (skinIndex in 0 until skinAmount) {
            pushLog("DAE Importer", "Parsing skin number $skinIndex")

            val skinCurrent: DaeSkinHandle = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — collada_db.getElement(skinIndex, COLLADA_TYPE_SKIN) not yet implemented")
                object : DaeSkinHandle {}
            }
            val skinGeom: DaeGeometryHandle = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — skin.getSource().getElement() as domGeometry not yet implemented")
                object : DaeGeometryHandle {}
            }
            val skinMesh: DaeMeshHandle = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — skinGeom.getMesh() not yet implemented")
                object : DaeMeshHandle {}
            }

            val currentObjectIter = meshUsageTracker.indexOfFirst { it === skinMesh }
            if (currentObjectIter < 0) {
                pushLog("DAE Importer", "Skin associated mesh has no equivalent loaded object mesh, skipping.")
                continue
            }
            if (currentObjectIter >= objectVector.size) {
                pushLog("DAE Importer", "Requested object out of bounds, skipping.")
                continue
            }

            val currentObject = objectVector[currentObjectIter]
            val skinSuccess = processSkin(colladaDom, skinMesh, skinCurrent, currentObject)
            if (skinSuccess) {
                ++skinsLoaded
                pushLog("DAE Importer", "Skin idx $skinIndex loading successful.")
            } else {
                pushLog("DAE Importer", "Skin idx $skinIndex loading unsuccessful.")
            }
            currentObject.logObjectInfo()
        }

        when {
            skinAmount > 0 && skinsLoaded == skinAmount ->
                pushLog("DAE Importer", "All available skin data has been successfully loaded..")
            skinAmount > 0 && skinsLoaded > 0 ->
                pushLog("DAE Importer", "$skinAmount Skin data instances found, out of these - $skinsLoaded loaded successfully.")
            skinAmount > 0 ->
                pushLog("DAE Importer", "Skinning data found, but all of it failed to load.")
            else ->
                pushLog("DAE Importer", "No skinning data found.")
        }

        return LoadFileReturn(true, mLoadingLog)
    }

    fun processObject(currentMesh: DaeMeshHandle, currentObject: LLLocalMeshObject): Boolean {
        val objectFaces = currentObject.getFaces(mLod)
        val triangleCount: Int = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentMesh.getTriangles_array().getCount() not yet implemented")
            0
        }
        val polylistCount: Int = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentMesh.getPolylist_array().getCount() not yet implemented")
            0
        }
        val totalFacesFound = triangleCount + polylistCount

        pushLog("DAE Importer", "Potentially $totalFacesFound object faces found.")

        if (totalFacesFound > LL_SCULPT_MESH_MAX_FACES) {
            pushLog("DAE Importer", "NOTE: object contains more than $LL_SCULPT_MESH_MAX_FACES faces, but ONLY up to $LL_SCULPT_MESH_MAX_FACES faces will be loaded.")
        }

        var submeshFailureFound = false
        var stopLoadingAdditionalFaces = false

        fun processSubmesh(idx: Int, arrayType: SubmeshType) {
            if (objectFaces.size >= LL_SCULPT_MESH_MAX_FACES) {
                pushLog("DAE Importer", "NOTE: reached the limit of $LL_SCULPT_MESH_MAX_FACES faces per object, ignoring the rest.")
                stopLoadingAdditionalFaces = true
                return
            }

            val submesh = LLLocalMeshFace()
            val success: Boolean = when (arrayType) {
                SubmeshType.TRIANGLE -> {
                    pushLog("DAE Importer", "Attempting to load face idx $idx of type TRIANGLES.")
                    val triangleRef: DaeTrianglesHandle = run {
                        System.err.println("LLLocalMeshImportDAE: use JVM equivalent — triangle_array.get(idx) not yet implemented")
                        object : DaeTrianglesHandle {}
                    }
                    readMesh_Triangle(submesh, triangleRef)
                }
                SubmeshType.POLYLIST -> {
                    pushLog("DAE Importer", "Attempting to load face idx $idx of type POLYLIST.")
                    val polylistRef: DaePolylistHandle = run {
                        System.err.println("LLLocalMeshImportDAE: use JVM equivalent — polylist_array.get(idx) not yet implemented")
                        object : DaePolylistHandle {}
                    }
                    readMesh_Polylist(submesh, polylistRef)
                }
                SubmeshType.POLYGONS -> {
                    pushLog("DAE Importer", "Attempting to load face idx $idx of type POLYGONS.")
                    pushLog("DAE Importer", "POLYGONS type schema is deprecated.")
                    false
                }
            }

            if (success) {
                pushLog("DAE Importer", "Face idx $idx loaded successfully.")
                objectFaces.add(submesh)
            } else {
                pushLog("DAE Importer", "Face idx $idx failed to load.")
                submeshFailureFound = true
            }
        }

        for (i in 0 until triangleCount) {
            if (stopLoadingAdditionalFaces) break
            processSubmesh(i, SubmeshType.TRIANGLE)
        }

        for (i in 0 until polylistCount) {
            if (stopLoadingAdditionalFaces) break
            processSubmesh(i, SubmeshType.POLYLIST)
        }

        return !submeshFailureFound
    }

    fun processSkin(
        colladaDoc: DaeDocument,
        currentMesh: DaeMeshHandle,
        currentSkin: DaeSkinHandle,
        currentObject: LLLocalMeshObject
    ): Boolean {
        pushLog("DAE Importer", "Preparing transformations and bind shape matrix.")

        val skinInfo = currentObject.getObjectMeshSkinInfo() ?: MeshSkinInfo()

        val normalizedTransformation = buildNormalizedTransformation(currentObject)
        val inverseNormalizedTransformation: FloatArray = run {
            System.err.println("LLLocalMeshImportDAE: GPU inverse(normalizedTransformation) not yet implemented")
            identityMatrix4x4()
        }

        val bindShapeMatrix: FloatArray? = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentSkin.getBind_shape_matrix() not yet implemented")
            null
        }
        if (bindShapeMatrix != null) {
            System.err.println("LLLocalMeshImportDAE: GPU load bind matrix from DAE, multiply normalizedTransformation * bindShapeMatrix -> skinInfo.bindShapeMatrix not yet implemented")
        }

        val jointMap = loadJointMap()

        pushLog("DAE Importer", "Preparing to process skeleton[s]...")

        val jointTransforms: JointTransformMap = mutableMapOf()
        val skeletonCount: Int = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — collada_db.getElementCount(\"skeleton\") not yet implemented")
            0
        }

        if (skeletonCount == 0) {
            pushLog("DAE Importer", "No conventional skeleton data found, attempting to recreate from joints...")
            val documentScene: DaeElementHandle = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — collada_document_root.getDescendant(\"visual_scene\") not yet implemented")
                object : DaeElementHandle {}
            }
            val sceneChildren: List<DaeNodeHandle> = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — documentScene.getChildren() as domNode list not yet implemented")
                emptyList()
            }
            for (child in sceneChildren) {
                processSkeletonJoint(child, jointMap, jointTransforms, recurseChildren = true)
            }
        } else {
            pushLog("DAE Importer", "Found $skeletonCount skeletons.")
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — iterate skeletons, use daeSIDResolver to find each joint by name, call processSkeletonJoint not yet implemented")
        }

        val jointInputs: List<DaeInputHandle> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentSkin.getJoints().getInput_array() not yet implemented")
            emptyList()
        }

        val processJointName = { jointName: String ->
            if (jointMap.containsKey(jointName)) {
                val mapped = jointMap[jointName]!!
                skinInfo.jointNames.add(mapped)
                skinInfo.jointNums.add(-1)
            }
        }

        for (input in jointInputs) {
            val semantic: String = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — input.getSemantic() not yet implemented")
                ""
            }
            val source: DaeSourceHandle = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — input.getSource().getElement() as domSource not yet implemented")
                object : DaeSourceHandle {}
            }

            when (semantic) {
                "JOINT" -> {
                    val nameArray: List<String>? = run {
                        System.err.println("LLLocalMeshImportDAE: use JVM equivalent — source.getName_array()?.getValue() not yet implemented")
                        null
                    }
                    if (nameArray != null) {
                        for (name in nameArray) processJointName(name)
                    } else {
                        val idArray: List<String>? = run {
                            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — source.getIDREF_array()?.getValue() mapped to IDs not yet implemented")
                            null
                        }
                        idArray?.forEach { processJointName(it) }
                            ?: pushLog("DAE Importer", "WARNING: Joint input did not provide name or ID, skipping.")
                    }
                }
                "INV_BIND_MATRIX" -> {
                    val floatValues: List<Float> = run {
                        System.err.println("LLLocalMeshImportDAE: use JVM equivalent — source.getFloat_array().getValue() not yet implemented")
                        emptyList()
                    }
                    var matIdx = 0
                    while (matIdx + 15 < floatValues.size) {
                        val mat = FloatArray(16)
                        for (i in 0 until 4) for (j in 0 until 4) mat[i + j * 4] = floatValues[matIdx + i + j * 4]
                        skinInfo.invBindMatrix.add(mat)
                        matIdx += 16
                    }
                }
            }
        }

        if (!enforceRigJointLimit("DAE Importer", currentObject, skinInfo, skinInfo.jointNames.size.toUInt())) {
            return false
        }

        val applyJointOffsets: Boolean = run {
            System.err.println("LLLocalMeshImportDAE: read FSLocalMeshApplyJointOffsets setting not yet implemented")
            false
        }
        if (applyJointOffsets) {
            for ((jointNameIdx, jointName) in skinInfo.jointNames.withIndex()) {
                if (!jointMap.containsKey(jointName)) {
                    pushLog("DAE Importer", "WARNING: Unknown joint named $jointName found, skipping over it.")
                    continue
                }
                if (skinInfo.invBindMatrix.size <= jointNameIdx) {
                    pushLog("DAE Importer", "WARNING: Requesting out of bounds joint named $jointName")
                    break
                }
                val newInverse = skinInfo.invBindMatrix[jointNameIdx].copyOf()
                val jointTranslation = jointTransforms[jointName]
                if (jointTranslation != null) {
                    System.err.println("LLLocalMeshImportDAE: GPU setTranslation of newInverse from jointTranslation not yet implemented")
                }
                skinInfo.alternateBindMatrix.add(newInverse)
            }
        }

        val bindCount = skinInfo.alternateBindMatrix.size
        if (bindCount > 0 && bindCount != skinInfo.jointNames.size) {
            pushLog("DAE Importer", "WARNING: ${skinInfo.jointNames.size} joints were found, but $bindCount bind matrices were made.")
        }

        val rawVertexArray: DaeVerticesHandle = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentMesh.getVertices() not yet implemented")
            object : DaeVerticesHandle {}
        }
        val transformedPositions = mutableListOf<FloatArray>()
        val vertexInputs: List<DaeInputHandle> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — rawVertexArray.getInput_array() not yet implemented")
            emptyList()
        }

        for (vertexInput in vertexInputs) {
            val semantic: String = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — vertexInput.getSemantic() not yet implemented")
                ""
            }
            if (semantic != "POSITION") continue

            val posSource: DaeSourceHandle = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — vertexInput.getSource().getElement() as domSource not yet implemented")
                object : DaeSourceHandle {}
            }
            val posArray: List<Float> = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — posSource.getFloat_array().getValue() not yet implemented")
                emptyList()
            }

            var i = 0
            while (i + 2 < posArray.size) {
                val posVec = floatArrayOf(posArray[i], posArray[i + 1], posArray[i + 2], 0f)
                System.err.println("LLLocalMeshImportDAE: GPU transform posVec by inverseNormalizedTransformation not yet implemented")
                transformedPositions.add(posVec)
                i += 3
            }
        }

        val currentWeights: DaeVertexWeightsHandle = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentSkin.getVertex_weights() not yet implemented")
            object : DaeVertexWeightsHandle {}
        }
        val weightInputs: List<DaeInputHandle> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentWeights.getInput_array() not yet implemented")
            emptyList()
        }
        var vertexWeights: List<Float>? = null

        for (wInput in weightInputs) {
            val semantic: String = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — wInput.getSemantic() not yet implemented")
                ""
            }
            if (semantic == "WEIGHT") {
                vertexWeights = run {
                    System.err.println("LLLocalMeshImportDAE: use JVM equivalent — wInput.getSource().getElement().getFloat_array().getValue() not yet implemented")
                    emptyList()
                }
                break
            }
        }

        if (vertexWeights == null) {
            pushLog("DAE Importer", "ERROR: Failed to find valid weight data, stopping.")
            return false
        }

        val vtxInfluenceCount: List<Int> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentWeights.getVcount().getValue() not yet implemented")
            emptyList()
        }
        val jointWeightIndices: List<Int> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentWeights.getV().getValue() not yet implemented")
            emptyList()
        }

        data class JointWeightEntry(val jointIdx: Int, val weight: Float)
        val skinweightData: MutableMap<Int, List<JointWeightEntry>> = mutableMapOf()

        if (vtxInfluenceCount.size > transformedPositions.size) {
            pushLog("DAE Importer", "WARNING: More weight entries (${vtxInfluenceCount.size}) than positions (${transformedPositions.size}).")
        }

        var jointWeightStrider = 0
        for (jointIdx in vtxInfluenceCount.indices) {
            val influencesCount = vtxInfluenceCount[jointIdx]
            val weightList = mutableListOf<JointWeightEntry>()

            for (k in 0 until influencesCount) {
                val vtxIdx = jointWeightIndices[jointWeightStrider++]
                val weightIdx = jointWeightIndices[jointWeightStrider++]
                if (vtxIdx == -1) continue
                weightList.add(JointWeightEntry(vtxIdx, vertexWeights[weightIdx]))
            }

            weightList.sortByDescending { it.weight }

            val sorted = mutableListOf<JointWeightEntry>()
            var total = 0f
            for (entry in weightList.take(4)) {
                if (entry.weight > 0f) { sorted.add(entry); total += entry.weight }
            }
            val scale = if (total != 0f) 1f / total else 1f
            val normalized = sorted.map { JointWeightEntry(it.jointIdx, it.weight * scale) }
            skinweightData[jointIdx] = normalized
        }

        val faces = currentObject.getFaces(mLod)
        for (face in faces) {
            val positions = face.getPositions()
            val weights = face.getSkin()

            for (currentPosition in positions) {
                val foundIdx = transformedPositions.indexOfFirst { internal ->
                    val eps = 1e-5f
                    kotlin.math.abs(currentPosition[0] - internal[0]) < eps &&
                    kotlin.math.abs(currentPosition[1] - internal[1]) < eps &&
                    kotlin.math.abs(currentPosition[2] - internal[2]) < eps
                }
                if (foundIdx < 0) continue

                val cjoints = skinweightData[foundIdx] ?: continue
                val unit = LLLocalMeshFace.LLLocalMeshSkinUnit()
                for (j in cjoints.indices.take(4)) {
                    unit.jointIndices[j] = cjoints[j].jointIdx
                    unit.jointWeights[j] = cjoints[j].weight.coerceIn(0f, 0.999f)
                }
                weights.add(unit)
            }
        }

        buildBindPoseMatrix(skinInfo)
        skinInfo.updateHash()
        currentObject.setObjectMeshSkinInfo(skinInfo)
        return true
    }

    fun processSkeletonJoint(
        currentNode: DaeNodeHandle,
        jointMap: MutableMap<String, String>,
        jointTransforms: JointTransformMap,
        recurseChildren: Boolean = false
    ): Boolean {
        val nodeName: String? = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentNode.getName() not yet implemented")
            null
        }
        if (nodeName == null) return false
        val nodeType: NodeType = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentNode.getType() not yet implemented")
            NodeType.NODE
        }
        if (nodeType != NodeType.JOINT) return false

        if (jointMap.containsKey(nodeName)) {
            val translation: FloatArray? = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — resolve ./translate or ./location or child translate or ./transform matrix or ./matrix from domNode not yet implemented")
                null
            }
            if (translation != null) {
                jointTransforms[nodeName] = translation
            }
        }

        if (recurseChildren) {
            val children: List<DaeNodeHandle> = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — currentNode.getChildren() as domNode list not yet implemented")
                emptyList()
            }
            for (child in children) {
                processSkeletonJoint(child, jointMap, jointTransforms, recurseChildren)
            }
        }

        return true
    }

    fun readMesh_CommonElements(
        inputs: DaeInputArrayHandle,
        offsetPosition: IntRef,
        offsetNormals: IntRef,
        offsetUvmap: IntRef,
        indexStride: IntRef,
        sourcePosition: Ref<DaeSourceHandle?>,
        sourceNormals: Ref<DaeSourceHandle?>,
        sourceUvmap: Ref<DaeSourceHandle?>
    ): Boolean {
        System.err.println("LLLocalMeshImportDAE: use JVM equivalent — iterate domInputLocalOffset_Array, find VERTEX/NORMAL/TEXCOORD offsets and their domSource references, compute indexStride, validate sourcePosition is non-null with float array not yet implemented")
        return false
    }

    fun getElementName(element: DaeElementHandle?, fallbackIndex: Int): String {
        fun askElement(el: DaeElementHandle): String {
            val nameAttr: String? = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — el.getAttribute(\"name\") not yet implemented")
                null
            }
            val idAttr: String? = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — el.getID() not yet implemented")
                null
            }
            return when {
                !nameAttr.isNullOrEmpty() && !idAttr.isNullOrEmpty() -> "$nameAttr | $idAttr"
                !nameAttr.isNullOrEmpty() -> nameAttr
                !idAttr.isNullOrEmpty() -> idAttr
                else -> ""
            }
        }

        if (element != null) {
            val result = askElement(element)
            if (result.isNotEmpty()) return result
            val parent: DaeElementHandle? = run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — element.getParent() not yet implemented")
                null
            }
            if (parent != null) {
                val parentResult = askElement(parent)
                if (parentResult.isNotEmpty()) return parentResult
            }
        }
        return "object_$fallbackIndex"
    }

    fun readMesh_Triangle(dataOut: LLLocalMeshFace, dataIn: DaeTrianglesHandle): Boolean {
        val inputs: DaeInputArrayHandle = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — dataIn.getInput_array() not yet implemented")
            object : DaeInputArrayHandle {}
        }
        val offsetPos = IntRef(); val offsetNorm = IntRef(); val offsetUv = IntRef(); val stride = IntRef()
        val srcPos = Ref<DaeSourceHandle?>(null)
        val srcNorm = Ref<DaeSourceHandle?>(null)
        val srcUv = Ref<DaeSourceHandle?>(null)

        if (!readMesh_CommonElements(inputs, offsetPos, offsetNorm, offsetUv, stride, srcPos, srcNorm, srcUv)) {
            pushLog("DAE Importer", "Collada file error, could not read array positions.")
            return false
        }

        val posValues: List<Float> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — srcPos.value.getFloat_array().getValue() not yet implemented")
            emptyList()
        }
        if (posValues.isEmpty()) {
            pushLog("DAE Importer", "Collada file error, vertex position array is empty.")
            return false
        }

        val normValues: List<Float> = srcNorm.value?.let {
            run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — it.getFloat_array().getValue() not yet implemented")
                emptyList<Float>()
            }
        } ?: emptyList()
        val uvValues: List<Float> = srcUv.value?.let {
            run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — it.getFloat_array().getValue() not yet implemented")
                emptyList<Float>()
            }
        } ?: emptyList()
        val triangleList: List<Int> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — dataIn.getP().getValue() not yet implemented")
            emptyList()
        }

        val listIndices = dataOut.getIndices()
        val listPositions = dataOut.getPositions()
        val listNormals = dataOut.getNormals()
        val listUvs = dataOut.getUVs()

        val initBbox = floatArrayOf(posValues[0], posValues[1], posValues[2], 0f)
        dataOut.setFaceBoundingBox(initBbox, true)

        data class RepeatData(val normal: FloatArray, val uv: FloatArray, val index: Int)
        val repeatMapPositions = mutableListOf<FloatArray>()
        val repeatMapData = mutableListOf<MutableList<RepeatData>>()

        var triIter = 0
        while (triIter < triangleList.size) {
            val posIdx = triangleList[triIter + offsetPos.value]
            val attrPosition = floatArrayOf(
                posValues[posIdx * 3], posValues[posIdx * 3 + 1], posValues[posIdx * 3 + 2], 0f
            )

            val attrNormal = if (normValues.isNotEmpty()) {
                val nIdx = triangleList[triIter + offsetNorm.value]
                floatArrayOf(normValues[nIdx * 3], normValues[nIdx * 3 + 1], normValues[nIdx * 3 + 2], 0f)
            } else floatArrayOf(0f, 0f, 1f, 0f)

            val attrUv = if (uvValues.isNotEmpty()) {
                val uIdx = triangleList[triIter + offsetUv.value]
                floatArrayOf(uvValues[uIdx * 2], uvValues[uIdx * 2 + 1])
            } else floatArrayOf(0f, 0f)

            var repeatFound = false
            val seekerIdx = repeatMapPositions.indexOfFirst { it.contentEquals(attrPosition) }
            if (seekerIdx >= 0) {
                for (repeatVtx in repeatMapData[seekerIdx]) {
                    if (!repeatVtx.normal.contentEquals(attrNormal)) continue
                    if (!repeatVtx.uv.contentEquals(attrUv)) continue

                    val repeatedIndex = repeatVtx.index
                    val size = listIndices.size
                    val check = size % 3
                    if ((check < 1 || listIndices[size - 1] != repeatedIndex) &&
                        (check < 2 || listIndices[size - 2] != repeatedIndex)) {
                        repeatFound = true
                        listIndices.add(repeatedIndex)
                    }
                    break
                }
            }

            if (!repeatFound) {
                if ((listPositions.size + 1) >= 65535) {
                    pushLog("DAE Importer", "Face error, too many vertices. Do NOT exceed 65535 vertices per face.")
                    return false
                }
                dataOut.setFaceBoundingBox(attrPosition)
                listPositions.add(attrPosition)
                if (normValues.isNotEmpty()) listNormals.add(attrNormal)
                if (uvValues.isNotEmpty()) listUvs.add(attrUv)
                val currentIndex = listPositions.size - 1
                listIndices.add(currentIndex)

                val newTrackable = RepeatData(attrNormal, attrUv, currentIndex)
                if (seekerIdx >= 0) repeatMapData[seekerIdx].add(newTrackable)
                else {
                    repeatMapPositions.add(attrPosition)
                    repeatMapData.add(mutableListOf(newTrackable))
                }
            }

            triIter += stride.value
        }

        if (listPositions.isEmpty()) {
            pushLog("DAE Importer", "Face error, no valid vertex positions found.")
            return false
        }

        return true
    }

    fun readMesh_Polylist(dataOut: LLLocalMeshFace, dataIn: DaePolylistHandle): Boolean {
        val inputs: DaeInputArrayHandle = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — dataIn.getInput_array() not yet implemented")
            object : DaeInputArrayHandle {}
        }
        val offsetPos = IntRef(); val offsetNorm = IntRef(); val offsetUv = IntRef(); val stride = IntRef()
        val srcPos = Ref<DaeSourceHandle?>(null)
        val srcNorm = Ref<DaeSourceHandle?>(null)
        val srcUv = Ref<DaeSourceHandle?>(null)

        if (!readMesh_CommonElements(inputs, offsetPos, offsetNorm, offsetUv, stride, srcPos, srcNorm, srcUv)) {
            pushLog("DAE Importer", "Collada file error, could not read array positions.")
            return false
        }

        val posValues: List<Float> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — srcPos.value.getFloat_array().getValue() not yet implemented")
            emptyList()
        }
        if (posValues.isEmpty()) {
            pushLog("DAE Importer", "Collada file error, vertex position array is empty.")
            return false
        }

        val normValues: List<Float> = srcNorm.value?.let {
            run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — it.getFloat_array().getValue() not yet implemented")
                emptyList<Float>()
            }
        } ?: emptyList()
        val uvValues: List<Float> = srcUv.value?.let {
            run {
                System.err.println("LLLocalMeshImportDAE: use JVM equivalent — it.getFloat_array().getValue() not yet implemented")
                emptyList<Float>()
            }
        } ?: emptyList()
        val listPrimitives: List<Int> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — dataIn.getVcount().getValue() not yet implemented")
            emptyList()
        }
        val vertexIndices: List<Int> = run {
            System.err.println("LLLocalMeshImportDAE: use JVM equivalent — dataIn.getP().getValue() not yet implemented")
            emptyList()
        }

        val listIndices = dataOut.getIndices()
        val listPositions = dataOut.getPositions()
        val listNormals = dataOut.getNormals()
        val listUvs = dataOut.getUVs()

        val initBbox = floatArrayOf(posValues[0], posValues[1], posValues[2], 0f)
        dataOut.setFaceBoundingBox(initBbox, true)

        data class RepeatData(val normal: FloatArray, val uv: FloatArray, val index: Int)
        val repeatMapPositions = mutableListOf<FloatArray>()
        val repeatMapData = mutableListOf<MutableList<RepeatData>>()

        var globalVtxIndex = 0

        for (primIdx in listPrimitives.indices) {
            val numVertices = listPrimitives[primIdx]
            var firstIdx = 0
            var lastIdx = 0

            for (vtxIter in 0 until numVertices) {
                val currentVtxIndex = globalVtxIndex
                globalVtxIndex += stride.value

                val posIdx = vertexIndices[currentVtxIndex + offsetPos.value]
                val attrPosition = floatArrayOf(
                    posValues[posIdx * 3], posValues[posIdx * 3 + 1], posValues[posIdx * 3 + 2], 0f
                )

                val attrNormal = if (normValues.isNotEmpty()) {
                    val nIdx = vertexIndices[currentVtxIndex + offsetNorm.value]
                    floatArrayOf(normValues[nIdx * 3], normValues[nIdx * 3 + 1], normValues[nIdx * 3 + 2], 0f)
                } else floatArrayOf(0f, 0f, 1f, 0f)

                val attrUv = if (uvValues.isNotEmpty()) {
                    val uIdx = vertexIndices[currentVtxIndex + offsetUv.value]
                    floatArrayOf(uvValues[uIdx * 2], uvValues[uIdx * 2 + 1])
                } else floatArrayOf(0f, 0f)

                var repeatFound = false
                val seekerIdx = repeatMapPositions.indexOfFirst { it.contentEquals(attrPosition) }
                if (seekerIdx >= 0) {
                    for (repeatVtx in repeatMapData[seekerIdx]) {
                        if (!repeatVtx.normal.contentEquals(attrNormal)) continue
                        if (!repeatVtx.uv.contentEquals(attrUv)) continue

                        repeatFound = true
                        val repeatedIndex = repeatVtx.index
                        when {
                            vtxIter == 0 -> firstIdx = repeatedIndex
                            vtxIter == 1 -> lastIdx = repeatedIndex
                            else -> {
                                listIndices.add(firstIdx)
                                listIndices.add(lastIdx)
                                listIndices.add(repeatedIndex)
                                lastIdx = repeatedIndex
                            }
                        }
                        break
                    }
                }

                if (!repeatFound) {
                    if ((listPositions.size + 1) >= 65535) {
                        pushLog("DAE Importer", "Face error, too many vertices. Do NOT exceed 65535 vertices per face.")
                        return false
                    }
                    dataOut.setFaceBoundingBox(attrPosition)
                    listPositions.add(attrPosition)
                    if (normValues.isNotEmpty()) listNormals.add(attrNormal)
                    if (uvValues.isNotEmpty()) listUvs.add(attrUv)
                    val currentIndex = listPositions.size - 1

                    when {
                        vtxIter == 0 -> firstIdx = currentIndex
                        vtxIter == 1 -> lastIdx = currentIndex
                        else -> {
                            listIndices.add(firstIdx)
                            listIndices.add(lastIdx)
                            listIndices.add(currentIndex)
                            lastIdx = currentIndex
                        }
                    }

                    val newTrackable = RepeatData(attrNormal, attrUv, currentIndex)
                    if (seekerIdx >= 0) repeatMapData[seekerIdx].add(newTrackable)
                    else {
                        repeatMapPositions.add(attrPosition)
                        repeatMapData.add(mutableListOf(newTrackable))
                    }
                }
            }

            if (listPositions.isEmpty()) {
                pushLog("DAE Importer", "Face error, no valid vertex positions found.")
                return false
            }
        }

        return true
    }

    private fun identityMatrix4x4(): FloatArray =
        floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)

    private fun multiplyMatrix4x4(a: FloatArray, b: FloatArray): FloatArray {
        System.err.println("LLLocalMeshImportDAE: GPU 4x4 column-major matrix multiplication a * b not yet implemented")
        return identityMatrix4x4()
    }
}

enum class UpAxisType { X_UP, Y_UP, Z_UP }
enum class SubmeshType { TRIANGLE, POLYLIST, POLYGONS }
enum class NodeType { JOINT, NODE }

class IntRef(var value: Int = 0)
class Ref<T>(var value: T)

interface DaeHandle
interface DaeDocument
interface DaeMeshHandle
interface DaeSkinHandle
interface DaeGeometryHandle
interface DaeSourceHandle
interface DaeElementHandle
interface DaeInputHandle
interface DaeInputArrayHandle
interface DaeTrianglesHandle
interface DaePolylistHandle
interface DaeNodeHandle
interface DaeVerticesHandle
interface DaeVertexWeightsHandle
