﻿
namespace DataStructures.Heaps
{
    public interface IMinHeap<T> where T : System.IComparable<T>
    {
        /// <summary>
        /// Returns the number of elements in heap
        /// </summary>
        int Count();

        /// <summary>
        /// Checks whether this heap is empty
        /// </summary>
        bool IsEmpty();

        /// <summary>
        /// Heapifies the specified newCollection. Overrides the current heap.
        /// </summary>
        /// <param name="newCollection">New collection.</param>
        void Heapify(System.Collections.Generic.IList<T> newCollection);

        /// <summary>
        /// Adding a new key to the heap.
        /// </summary>
        /// <param name="heapKey">Heap key.</param>
        void Insert(T heapKey);

        /// <summary>
        /// Find the minimum node of a min heap.
        /// </summary>
        /// <returns>The minimum.</returns>
        T Peek();

        /// <summary>
        /// Removes the node of minimum value from a min heap.
        /// </summary>
        void RemoveMin();

        /// <summary>
        /// Returns the node of minimum value from a min heap after removing it from the heap.
        /// </summary>
        /// <returns>The min.</returns>
        T ExtractMin();

        /// <summary>
        /// Clear this heap.
        /// </summary>
        void Clear();

        /// <summary>
        /// Returns an array version of this heap.
        /// </summary>
        /// <returns>The array.</returns>
        T[] ToArray();

        /// <summary>
        /// Returns a list version of this heap.
        /// </summary>
        /// <returns>The list.</returns>
        System.Collections.Generic.List<T> ToList();

        /// <summary>
        /// Returns a new min heap that contains all elements of this heap.
        /// </summary>
        /// <returns>The min heap.</returns>
        MaxHeap<T> ToMaxHeap();
    }
}
