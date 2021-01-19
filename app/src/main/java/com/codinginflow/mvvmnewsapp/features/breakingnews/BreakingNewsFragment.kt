package com.codinginflow.mvvmnewsapp.features.breakingnews

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.codinginflow.mvvmnewsapp.MainActivity
import com.codinginflow.mvvmnewsapp.R
import com.codinginflow.mvvmnewsapp.databinding.FragmentBreakingNewsBinding
import com.codinginflow.mvvmnewsapp.core.shared.NewsListAdapter
import com.codinginflow.mvvmnewsapp.util.Resource
import com.codinginflow.mvvmnewsapp.util.showSnackbar
import com.codinginflow.mvvmnewsapp.util.viewBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news),
    MainActivity.OnBottomNavigationFragmentSelected {

    private val viewModel: BreakingNewsViewModel by viewModels()

    private lateinit var newsAdapter: NewsListAdapter

    private val binding by viewBinding(FragmentBreakingNewsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding

        newsAdapter = NewsListAdapter(
            onItemClick = { article ->
                val uri = Uri.parse(article.url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                binding.root.context.startActivity(intent)
            },
            onBookmarkClick = { article ->
                viewModel.onBookmarkClick(article)
            }
        )

        binding.apply {
            recyclerView.apply {
                setHasFixedSize(true)
                adapter = newsAdapter
                layoutManager = LinearLayoutManager(requireContext())
                itemAnimator?.changeDuration = 0
            }

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.onManualRefresh()
            }
        }

        viewModel.breakingNews.observe(viewLifecycleOwner) { result ->
            binding.swipeRefreshLayout.isRefreshing = result is Resource.Loading
            binding.recyclerView.isVisible = !result.data.isNullOrEmpty()
            binding.textViewError.isVisible = result.throwable != null && result.data.isNullOrEmpty()
            binding.textViewError.text = result.throwable?.localizedMessage ?: "An unknown error occurred"

            newsAdapter.submitList(result.data)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.events.collect { event ->
                when (event) {
                    is BreakingNewsViewModel.Event.ShowErrorMessage -> {
                        showSnackbar(
                            event.throwable.localizedMessage ?: "An unknown error occurred"
                        )
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    fun scrollUpAndRefresh() {
        binding.recyclerView.scrollToPosition(0)
        viewModel.onManualRefresh()
    }

    override fun onBottomNavigationFragmentSelected() {
        scrollUpAndRefresh()
    }
}